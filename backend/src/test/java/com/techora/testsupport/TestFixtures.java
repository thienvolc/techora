package com.techora.testsupport;

import com.techora.catalog.domain.entity.CategoryEntity;
import com.techora.catalog.domain.entity.ProductEntity;
import com.techora.catalog.domain.valueobject.ProductStatus;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.infra.persistence.OrderItemJpaEntity;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.domain.valueobject.PaymentStatus;
import com.techora.payment.infra.persistence.PaymentAttemptJpaEntity;
import com.techora.payment.infra.persistence.PaymentJpaEntity;
import com.techora.user.entity.UserEntity;
import com.techora.user.entity.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class TestFixtures {

    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private TestFixtures() {
    }

    public static UserEntity user() {
        long sequence = nextSequence();
        return UserEntity.builder()
                .username("user-" + sequence)
                .passwordHash("password-hash")
                .role(UserRole.USER)
                .createdAt(BASE_TIME)
                .build();
    }

    public static UserEntity admin() {
        long sequence = nextSequence();
        return UserEntity.builder()
                .username("admin-" + sequence)
                .passwordHash("password-hash")
                .role(UserRole.ADMIN)
                .createdAt(BASE_TIME)
                .build();
    }

    public static CategoryEntity category() {
        long sequence = nextSequence();
        return CategoryEntity.builder()
                .name("Category " + sequence)
                .slug("category-" + sequence)
                .description("Test category")
                .active(true)
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();
    }

    public static ProductEntity product(CategoryEntity category) {
        long sequence = nextSequence();
        return ProductEntity.builder()
                .name("Product " + sequence)
                .sku("SKU-" + sequence)
                .slug("product-" + sequence)
                .description("Test product")
                .price(BigDecimal.valueOf(100_00, 2))
                .status(ProductStatus.ACTIVE)
                .category(category)
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();
    }

    public static InventoryItemEntity inventoryItem(UUID productId, int quantityOnHand) {
        return InventoryItemEntity.builder()
                .productId(productId)
                .quantityOnHand(quantityOnHand)
                .reservedQuantity(0)
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();
    }

    public static InventoryReservationEntity reservedInventoryReservation(UUID orderId,
                                                                          UUID productId,
                                                                          int quantity,
                                                                          Instant expiresAt) {

        return InventoryReservationEntity.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(InventoryReservationStatus.RESERVED)
                .expiresAt(expiresAt)
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();
    }

    public static OrderJpaEntity order(UserEntity user, UUID productId) {
        long sequence = nextSequence();
        BigDecimal unitPrice = BigDecimal.valueOf(100_00, 2);

        OrderJpaEntity order = OrderJpaEntity.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .total(unitPrice)
                .paymentDeadlineAt(BASE_TIME.plusSeconds(3600))
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();

        OrderItemJpaEntity item = OrderItemJpaEntity.builder()
                .order(order)
                .productId(productId)
                .productName("Product " + sequence)
                .sku("SKU-" + sequence)
                .unitPrice(unitPrice)
                .quantity(1)
                .subtotal(unitPrice)
                .build();

        order.getItems().add(item);
        return order;
    }

    public static PaymentJpaEntity payment(UUID orderId, UUID userId) {
        return PaymentJpaEntity.builder()
                .orderId(orderId)
                .userId(userId)
                .username("user")
                .amount(BigDecimal.valueOf(100_00, 2))
                .status(PaymentStatus.PENDING)
                .orderPaymentDeadlineAt(BASE_TIME.plusSeconds(3600))
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();
    }

    public static PaymentAttemptJpaEntity paymentAttempt(UUID paymentId, UUID orderId, UUID userId) {
        long sequence = nextSequence();
        return PaymentAttemptJpaEntity.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .userId(userId)
                .providerName(PaymentProvider.VNPAY.name())
                .providerReference("VNPAY-" + sequence)
                .amount(BigDecimal.valueOf(100_00, 2))
                .status(PaymentAttemptStatus.PENDING)
                .expiresAt(BASE_TIME.plusSeconds(900))
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .build();
    }

    public static OutboxEventEntity pendingPaymentOutboxEvent(UUID aggregateId, OutboxEventType eventType) {
        UUID eventId = UUID.randomUUID();
        return OutboxEventEntity.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .aggregateType(OutboxAggregateType.PAYMENT)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .topic("techora.payment.events")
                .messageKey(aggregateId.toString())
                .eventVersion(1)
                .headers("{}")
                .payload("{\"eventId\":\"" + eventId + "\"}")
                .status(OutboxEventStatus.PENDING)
                .retryCount(0)
                .createdAt(BASE_TIME)
                .updatedAt(BASE_TIME)
                .nextAttemptAt(BASE_TIME)
                .build();
    }

    private static long nextSequence() {
        return SEQUENCE.incrementAndGet();
    }
}
