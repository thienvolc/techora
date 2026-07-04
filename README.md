# Techora

Techora is a Java 21 / Spring Boot backend for an e-commerce system.

The project focuses on real commerce failure modes: duplicate payment callbacks, overselling, retrying client requests, stale catalog reads, and unreliable event publishing.

It follows a domain-oriented Clean Architecture.

## Tech Stack

| Technology | Usage |
|---|---|
| Java 21, Spring Boot, Spring Web | Backend API |
| PostgreSQL, Spring Data JPA | Primary database and persistence |
| Spring Security, JWT | Authentication and authorization |
| Redis | L2 cache for product reads; temporary bypass when Redis is unhealthy |
| Caffeine | L1 local cache and single-flight protection for hot product keys |
| Kafka, Spring Kafka | Publish payment integration events from outbox |
| Transactional Outbox | Durable payment event relay with retry and stale-lock recovery |
| VNPAY | Payment URL creation, IPN verification, and return-url read flow |
| ShedLock | Guards scheduled expiration/reconciliation jobs |
| Resilience4j RateLimiter | Rate limit public catalog browsing |
| Actuator, Micrometer | Cache, outbox, and inventory reconciliation metrics |
| Docker Compose | Local PostgreSQL, Redis, and Kafka |

## Architecture Overview

```text
controller/      HTTP boundary
application/     use cases, commands, queries, ports, application models
domain/          entities, value objects, policies, domain events
infra/           external integrations such as VNPAY, Kafka, Redis adapters
persistence/     JPA entities, repositories, mappers
```

Cross-domain calls use ports, commands, snapshots, and events instead of passing mutable JPA entities across module boundaries.

## Core Features

### 1. Product Read Model and Cache Strategy

**Problem**

Product browsing is read-heavy, but product data is not owned by one table in practice. Catalog owns product/category data, while inventory owns stock. Joining catalog, category, and inventory for every public browse request makes the hot path slower, harder to cache, and more coupled.

**Solution**

Techora keeps a product read model for browse/detail APIs. The read model stores the fields needed by the storefront, including catalog data and current available stock.

The projection is updated from internal events:

1. Product/category changes update the catalog projection.
2. Inventory stock changes update only the stock portion of the product read model.
3. Product detail cache is precisely evicted when the product or stock changes.
4. Product listing cache is TTL-oriented because short-lived staleness is acceptable for browsing.

Reads use a two-level cache:

1. Caffeine is the local L1 cache for fast hot-key reads.
2. Redis is the shared L2 cache across app instances.
3. Redis failures trigger temporary bypass instead of taking down the API.
4. Local single-flight protection prevents many concurrent requests from rebuilding the same hot key.

**Result**

Browse APIs avoid repeated expensive joins while still exposing stock-aware product data. Detail reads stay safer through precise invalidation, while listing reads trade short stale windows for throughput.

**Tradeoff**

The read model is eventually consistent. If projection sync fails or drifts, it needs reconciliation or rebuild tooling; the browse path intentionally does not hit the write model on every request.

### 2. Reliable Outbox Relay

**Problem**

Payment events must not be lost when Kafka is unavailable. Publishing directly to Kafka inside the payment flow is risky: if the database commit succeeds but Kafka publish fails, the system loses the integration event.

**Solution**

Techora stores payment integration events in an `outbox_events` table first. A relay worker later publishes those rows to Kafka.

The relay works in batches:

1. Claim pending rows by moving them from `PENDING` to `PROCESSING` with PostgreSQL `FOR UPDATE SKIP LOCKED`, so multiple app instances can relay events without processing the same row.
2. Publish claimed events to Kafka asynchronously.
3. Convert publish results into a single state outcome: success becomes `PUBLISHED`, temporary failure becomes `PENDING` with `next_attempt_at`, exhausted retries become `FAILED`.
4. Update the database in bulk. The hot success path uses PostgreSQL `RETURNING id`, so the app knows exactly which rows were applied without issuing one update per row.
5. A stale-lock housekeeper releases rows stuck in `PROCESSING` when a worker crashes before updating the result.

**Result**

Payment integration event delivery is decoupled from the request transaction. Kafka downtime does not silently lose events; failed publishes are retried with backoff, and stuck processing rows can recover automatically.

**Tradeoff**

This provides at-least-once delivery, not exactly-once delivery. A message can be published to Kafka and then retried if the worker crashes before marking the row as `PUBLISHED`, so downstream consumers must be idempotent by `eventId`.

### 3. Provider Payment Result Processing

**Problem**

Payment gateways can send duplicate, late, failed, or amount-mismatched callbacks. A browser return URL cannot be used as payment truth, and a repeated IPN must not transition the same payment or order twice.

**Solution**

Techora treats VNPAY IPN as the only write path for provider payment results. The return URL is read-only and only reports the latest known payment state.

The provider result lifecycle is handled in ordered steps:

1. Verify the VNPAY secure hash before parsing or trusting provider fields.
2. Resolve the `PaymentAttempt` by provider reference.
3. Lock the parent `Payment` before locking the `PaymentAttempt` to keep lock ordering deterministic.
4. Stop early if the attempt already has provider evidence, making duplicate IPNs idempotent.
5. Store provider evidence on the attempt: response code, provider status, transaction id, raw payload, received time, and amount.
6. Apply provider failure only if the attempt is still pending.
7. Apply provider success only if the amount matches and the attempt is still payable.
8. Move amount mismatch, late success, or unsafe payment state into reconciliation instead of forcing the order to paid.

**Result**

Duplicate IPNs, late success callbacks, and amount mismatch cases do not double-transition payment/order state or hide money/order inconsistencies.

**Tradeoff**

Unsafe provider results require reconciliation. The system favors explicit review over automatically marking an ambiguous payment as paid.

### 4. Inventory Reservation as Stock Ownership Boundary

**Problem**

Product data and stock data have different ownership. Catalog can describe a product, but inventory must be the only module that decides whether stock can be reserved, confirmed, or released. If catalog/order/payment mutate stock directly, overselling and hidden coupling become likely.

**Solution**

Techora makes inventory the owner of stock state. Catalog exposes product snapshots, but stock lives in `InventoryItem`.

Inventory separates stock into two numbers:

```text
quantityOnHand     physical stock owned by inventory
reservedQuantity   stock currently held by unpaid orders
```

Reservation rows are the audit trail for why stock is currently held:

1. Checkout sends a reserve-inventory command with order id and product quantities.
2. Inventory creates one reservation row per order/product.
3. Inventory locks stock rows by product id before changing `reservedQuantity`.
4. Reservation items are processed in deterministic product order to reduce cross-order deadlocks.
5. Order paid event confirms reservations and reduces `quantityOnHand`.
6. Order cancelled/expired event releases reservations back to available stock.
7. A reconciliation job compares `inventory_items.reservedQuantity` with active reservation rows to detect drift.

**Result**

Stock changes stay inside the inventory boundary. Order and payment can drive the lifecycle, but they do not own inventory counters. The system can explain both "how much stock is available" and "why stock is currently reserved".

**Tradeoff**

The model stores a denormalized `reservedQuantity` counter for fast availability checks. A reconciliation job exists because denormalized counters can drift; current reconciliation is detect-only and does not auto-repair without operator review.

### 5. Idempotent Command Execution

**Problem**

Checkout and payment initiation are retry-prone commands. Clients may retry after a timeout, mobile network drop, browser double-click, or gateway latency. The retry must not create another order, another payment attempt, or another state transition.

**Solution**

Techora wraps retry-sensitive commands with an idempotency executor. The executor stores the command lifecycle separately from the business aggregate.

The command execution flow is:

1. Build an idempotency command from user id, operation, key, request path, HTTP method, params/body, and response type.
2. Normalize request params by sorting keys before hashing, so equivalent requests produce the same fingerprint.
3. Create an idempotency record for the key, operation, user, and request fingerprint.
4. Execute the business command only when the idempotency record is new or eligible.
5. Store the serialized response when the command completes.
6. Replay the stored response when the same key and same fingerprint are sent again.
7. Reject the request when the same key is reused with a different fingerprint.

Response serialization is isolated in a codec, so the idempotency executor does not need to know the shape of checkout or payment responses.

**Result**

Retrying a command returns the same business result instead of running the command again. The idempotency layer absorbs client/network retries while the domain still owns state transition rules.

**Tradeoff**

Idempotency is not used as the only correctness mechanism. Domain rules, payment attempt state, order status transitions, and database constraints still protect the business model when requests race or provider callbacks arrive later.

### 6. Payment Attempt Lifecycle

**Problem**

A business payment is not the same thing as a provider attempt. A user may reopen the payment page, retry payment after a timeout, receive a late provider callback, or hit an amount mismatch. Treating all of that as one `payment` row makes the lifecycle ambiguous.

**Solution**

Techora models payment in two layers:

```text
Payment          business payment state for the order
PaymentAttempt   concrete provider attempt with reference, deadline, status, and provider evidence
```

The initiation flow keeps order/payment coupling explicit:

1. Payment asks the order module to prepare the order through `OrderPaymentPort`.
2. The order module locks the payable order and marks it payment-pending according to order rules.
3. Payment locks an existing payment by order and user.
4. If a pending payment and reusable pending attempt already exist, the system returns that attempt instead of creating another provider reference.
5. If no payment exists, the system creates one `Payment` and the first `PaymentAttempt`.
6. The attempt receives its own provider reference and provider-specific expiration window.

The provider callback then updates the attempt first. Only a safe attempt result can update the parent payment and confirm the order. Unsafe results keep provider evidence and move the payment into reconciliation.

**Result**

The system can distinguish "this order is paid" from "this provider attempt received a result". Reopening payment can reuse a valid pending attempt, while expired, failed, late, or mismatched attempts remain auditable.

**Tradeoff**

Payment attempts make the model larger than a single payment table, but they make retry, expiry, provider evidence, and reconciliation explicit instead of hiding them inside one mutable status.

### 7. Order-Payment-Inventory Boundary

**Problem**

Checkout touches three sensitive domains at once: order, payment, and inventory. If one service loads and mutates all entities directly, the system becomes hard to reason about and easy to break when payment callbacks, order timeout, or inventory release happen concurrently.

**Solution**

Techora keeps each domain responsible for its own state and connects them through ports, commands, snapshots, and internal events.

The boundary is designed around ownership:

1. Order owns the order lifecycle and payment deadline.
2. Payment owns payment state, attempts, provider reference, and provider evidence.
3. Inventory owns stock counters and reservation rows.

The payment timeout flow shows the boundary:

1. Order finds `PAYMENT_PENDING` orders whose `paymentDeadlineAt` has passed.
2. Order asks payment to expire the pending payment through a payment port.
3. Order cancels itself only when payment returns `EXPIRED`.
4. Order does not cancel if payment is already paid, not pending, or not found.
5. Order cancellation publishes an internal order event.
6. Inventory listens to the order event and releases reserved stock inside the inventory boundary.

The payment success flow follows the same rule: payment confirms provider success, then order confirms the order through an order port, and inventory confirms reserved stock from the order-paid event.

**Result**

Order, payment, and inventory can change together as a workflow without sharing mutable persistence entities. Each module protects its own invariants while still participating in the checkout/payment lifecycle.

**Tradeoff**

The boundary adds more coordination code than a single service mutating everything directly. The benefit is that timeout, provider callback, and inventory release rules remain explicit instead of being hidden behind JPA dirty checking across modules.
