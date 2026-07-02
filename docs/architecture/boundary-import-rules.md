# Boundary Import Rules

This project is migrating toward feature-first bounded contexts. These rules freeze the most important boundaries before the remaining refactors happen.

## Phase 1 rules

- `checkout/application` must not import `checkout/controller`.
- `checkout/application` must not import concrete `order.application.service` or `inventory.application.service` classes.
- `checkout` must not import `order.domain.entity` types.
- `payment` must not import `order.domain.entity` types.
- `inventory/application` must not import `catalog.dto.response` HTTP DTOs.
- `inventory/application` must not import concrete `catalog.service` classes.

## Phase 2 rules

- `inventory/application` must not import `catalog.projection.event` classes.
- `catalog/service` and `catalog/projection` must not import concrete `inventory.application.service` classes.
- Inventory publishes inventory-owned events, such as `InventoryStockChangedEvent`.
- Catalog projection consumes inventory events and reads stock through `CatalogInventoryPort`.

## DTO boundary rules

- Controller request/response DTOs must stay in `controller/request` and `controller/response`.
- Legacy module DTOs in `dto/request` and `dto/response` are still guarded as HTTP DTOs.
- HTTP request/response DTOs must not import domain entity, domain value object, or persistence entity types.
- Application command/result records stay in `application/command` and `application/result`.
- Application code must not import controller request/response DTOs.
- Services and mappers must not return or import module HTTP response DTOs. They should return application results and let controllers map `Result -> Response`.
- Existing DTO enum leaks are tracked as baseline debt in `backend/scripts/check-boundary-imports.ps1`.

## Why this exists

The goal is to stop new coupling while the current legacy imports are removed gradually. Existing violations are listed as baseline entries in `backend/scripts/check-boundary-imports.ps1`.

Run the guard:

```powershell
.\scripts\check-boundary-imports.ps1
```

Run strict mode to see current baseline debt as failures:

```powershell
.\scripts\check-boundary-imports.ps1 -Strict
```

## Migration direction

- Controller request/response DTOs stay at the HTTP boundary.
- Application use cases return application results, not controller responses.
- Cross-module communication uses ports with command/result/snapshot records.
- Domain entities and JPA entities do not cross module boundaries.
- Query flows stay simple. Prefer `*QueryService` or existing read-model services for read-only access instead of wrapping every query in a handler.
- Command flows may use explicit use cases/handlers because they own state changes, side effects, idempotency, and domain events.

## Query convention

Queries are allowed to be boring:

```text
Controller -> QueryService -> Repository/ReadModel -> Result/Response
```

Do not add ports, handlers, events, or command objects to simple reads unless the read crosses a bounded-context boundary or needs a stable application contract.

Commands should stay explicit:

```text
Controller -> Command -> UseCase/Handler -> Domain/Application Service -> Result
```

This keeps the code mature without turning every read endpoint into ceremony.
