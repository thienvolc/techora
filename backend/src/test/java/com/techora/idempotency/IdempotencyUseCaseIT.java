package com.techora.idempotency;

import com.techora.cart.entity.CartEntity;
import com.techora.cart.entity.CartItemEntity;
import com.techora.cart.repository.CartRepository;
import com.techora.catalog.domain.entity.CategoryEntity;
import com.techora.catalog.domain.entity.ProductEntity;
import com.techora.catalog.persistence.repository.CategoryRepository;
import com.techora.catalog.persistence.repository.ProductRepository;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.idempotency.entity.IdempotencyKeyEntity;
import com.techora.idempotency.entity.IdempotencyStatus;
import com.techora.idempotency.repository.IdempotencyKeyRepository;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.model.PlaceOrderView;
import com.techora.order.application.usecase.PlaceOrderUseCase;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
import com.techora.payment.application.command.InitiateVnPayPaymentCommand;
import com.techora.payment.application.model.VnPayPaymentSession;
import com.techora.payment.application.usecase.InitiateVnPayPaymentUseCase;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import com.techora.payment.infra.persistence.PaymentAttemptJpaEntity;
import com.techora.payment.infra.persistence.PaymentAttemptJpaRepository;
import com.techora.payment.infra.persistence.PaymentJpaEntity;
import com.techora.payment.infra.persistence.PaymentJpaRepository;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import com.techora.user.entity.UserEntity;
import com.techora.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyUseCaseIT extends AbstractIntegrationTest {

    private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final String CLIENT_IP = "127.0.0.1";

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

    @Autowired
    private InitiateVnPayPaymentUseCase initiateVnPayPaymentUseCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private PaymentAttemptJpaRepository paymentAttemptRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Test
    void sameIdempotencyKeyAndSamePlaceOrderCommandReplaysStoredResult() {
        PlaceOrderScenario scenario = seedPlaceOrderScenario(2, 10);
        String idempotencyKey = "checkout-" + UUID.randomUUID();
        PlaceOrderCommand command = new PlaceOrderCommand(
                scenario.user().getId(),
                CLIENT_IP,
                idempotencyKey
        );

        PlaceOrderView firstResult = placeOrderUseCase.execute(command);
        PlaceOrderView replayedResult = placeOrderUseCase.execute(command);

        assertThat(replayedResult).isEqualTo(firstResult);
        assertThat(ordersOf(scenario.user().getId())).hasSize(1);
        assertThat(paymentsForOrder(firstResult.id())).hasSize(1);
        assertThat(paymentAttemptsForPayment(firstResult.paymentId())).hasSize(1);
        assertThat(reservationRepository.findByOrderIdOrderByCreatedAtAsc(firstResult.id())).hasSize(1);
        assertThat(cartRepository.findWithItemsByUserId(scenario.user().getId()).orElseThrow().getItems())
                .isEmpty();
        assertCompletedIdempotencyKey(scenario.user().getId(), idempotencyKey);
    }

    @Test
    void expiredIdempotencyKeyDoesNotReplayStoredResult() {
        PlaceOrderScenario scenario = seedPlaceOrderScenario(2, 10);
        String idempotencyKey = "checkout-" + UUID.randomUUID();
        PlaceOrderCommand command = new PlaceOrderCommand(
                scenario.user().getId(),
                CLIENT_IP,
                idempotencyKey
        );

        PlaceOrderView firstResult = placeOrderUseCase.execute(command);
        IdempotencyKeyEntity key = idempotencyKeyOf(scenario.user().getId(), idempotencyKey);
        key.setExpiresAt(Instant.now().minusSeconds(60));
        idempotencyKeyRepository.saveAndFlush(key);

        assertThatThrownBy(() -> placeOrderUseCase.execute(command))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.IDEMPOTENCY_KEY_EXPIRED));

        assertThat(ordersOf(scenario.user().getId())).hasSize(1);
        assertThat(paymentsForOrder(firstResult.id())).hasSize(1);
        assertThat(paymentAttemptsForPayment(firstResult.paymentId())).hasSize(1);
        assertCompletedIdempotencyKey(scenario.user().getId(), idempotencyKey);
    }

    @Test
    void failedCommandDoesNotPersistIdempotencyKey() {
        UserEntity user = userRepository.save(TestFixtures.user());
        String idempotencyKey = "checkout-" + UUID.randomUUID();
        PlaceOrderCommand command = new PlaceOrderCommand(
                user.getId(),
                CLIENT_IP,
                idempotencyKey
        );

        assertThatThrownBy(() -> placeOrderUseCase.execute(command))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.CART_EMPTY));

        assertThat(idempotencyKeysOf(user.getId(), idempotencyKey)).isEmpty();
    }

    @Test
    void sameIdempotencyKeyWithDifferentPaymentInitiationPayloadReturnsConflict() {
        UserEntity user = userRepository.save(TestFixtures.user());
        OrderJpaEntity firstOrder = seedStockReservedOrder(user);
        OrderJpaEntity secondOrder = seedStockReservedOrder(user);
        String idempotencyKey = "payment-" + UUID.randomUUID();

        initiateVnPayPaymentUseCase.execute(initiatePaymentCommand(user.getId(), firstOrder.getId(), idempotencyKey));

        assertThatThrownBy(() ->
                initiateVnPayPaymentUseCase.execute(initiatePaymentCommand(user.getId(), secondOrder.getId(), idempotencyKey)))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getResponseCode()).isEqualTo(ResponseCode.IDEMPOTENCY_KEY_CONFLICT));

        assertThat(paymentsForOrder(firstOrder.getId())).hasSize(1);
        assertThat(paymentsForOrder(secondOrder.getId())).isEmpty();
        assertThat(reloadOrder(secondOrder).getStatus())
                .isEqualTo(OrderStatus.STOCK_RESERVED);
        assertCompletedIdempotencyKey(user.getId(), idempotencyKey);
    }

    @Test
    void repeatedPaymentInitiationForSameOrderReturnsExistingPendingPaymentSession() {
        UserEntity user = userRepository.save(TestFixtures.user());
        OrderJpaEntity order = seedStockReservedOrder(user);
        String firstKey = "payment-" + UUID.randomUUID();
        String secondKey = "payment-" + UUID.randomUUID();

        VnPayPaymentSession firstSession =
                initiateVnPayPaymentUseCase.execute(initiatePaymentCommand(user.getId(), order.getId(), firstKey));
        VnPayPaymentSession secondSession =
                initiateVnPayPaymentUseCase.execute(initiatePaymentCommand(user.getId(), order.getId(), secondKey));

        assertThat(secondSession.paymentId()).isEqualTo(firstSession.paymentId());
        assertThat(secondSession.paymentUrl()).isEqualTo(firstSession.paymentUrl());
        assertThat(Math.abs(ChronoUnit.MILLIS.between(firstSession.expiresAt(), secondSession.expiresAt())))
                .isLessThanOrEqualTo(1);
        assertThat(paymentsForOrder(order.getId())).hasSize(1);
        assertThat(paymentAttemptsForPayment(firstSession.paymentId())).hasSize(1);
        assertThat(reloadOrder(order).getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertCompletedIdempotencyKey(user.getId(), firstKey);
        assertCompletedIdempotencyKey(user.getId(), secondKey);
    }

    @Test
    void expiredPaymentAttemptIsNotReusedWhenInitiatingPaymentAgain() {
        UserEntity user = userRepository.save(TestFixtures.user());
        OrderJpaEntity order = seedStockReservedOrder(user);
        String firstKey = "payment-" + UUID.randomUUID();
        String secondKey = "payment-" + UUID.randomUUID();

        VnPayPaymentSession firstSession =
                initiateVnPayPaymentUseCase.execute(initiatePaymentCommand(user.getId(), order.getId(), firstKey));
        PaymentAttemptJpaEntity firstAttempt = paymentAttemptsForPayment(firstSession.paymentId()).getFirst();
        String firstProviderReference = firstAttempt.getProviderReference();

        firstAttempt.setStatus(PaymentAttemptStatus.EXPIRED);
        firstAttempt.setExpiredAt(Instant.now().minusSeconds(60));
        paymentAttemptRepository.saveAndFlush(firstAttempt);

        VnPayPaymentSession secondSession =
                initiateVnPayPaymentUseCase.execute(initiatePaymentCommand(user.getId(), order.getId(), secondKey));
        List<PaymentAttemptJpaEntity> attempts = paymentAttemptsForPayment(firstSession.paymentId());
        PaymentAttemptJpaEntity pendingAttempt = attempts.stream()
                .filter(attempt -> attempt.getStatus() == PaymentAttemptStatus.PENDING)
                .findFirst()
                .orElseThrow();

        assertThat(secondSession.paymentId()).isEqualTo(firstSession.paymentId());
        assertThat(secondSession.paymentUrl()).isNotEqualTo(firstSession.paymentUrl());
        assertThat(paymentsForOrder(order.getId())).hasSize(1);
        assertThat(attempts).hasSize(2);
        assertThat(attempts)
                .anySatisfy(attempt -> {
                    assertThat(attempt.getId()).isEqualTo(firstAttempt.getId());
                    assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.EXPIRED);
                    assertThat(attempt.getProviderReference()).isEqualTo(firstProviderReference);
                });
        assertThat(pendingAttempt.getId()).isNotEqualTo(firstAttempt.getId());
        assertThat(pendingAttempt.getProviderReference()).isNotEqualTo(firstProviderReference);
        assertThat(reloadOrder(order).getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertCompletedIdempotencyKey(user.getId(), firstKey);
        assertCompletedIdempotencyKey(user.getId(), secondKey);
    }

    private PlaceOrderScenario seedPlaceOrderScenario(int quantity, int stockQuantity) {
        UserEntity user = userRepository.save(TestFixtures.user());
        CategoryEntity category = categoryRepository.save(TestFixtures.category());
        ProductEntity product = productRepository.save(TestFixtures.product(category));
        inventoryItemRepository.save(TestFixtures.inventoryItem(product.getId(), stockQuantity));
        seedCart(user, product, quantity);

        return new PlaceOrderScenario(user, product);
    }

    private void seedCart(UserEntity user, ProductEntity product, int quantity) {
        CartEntity cart = CartEntity.builder()
                .user(user)
                .createdAt(TEST_TIME)
                .updatedAt(TEST_TIME)
                .build();

        CartItemEntity item = CartItemEntity.builder()
                .cart(cart)
                .product(product)
                .quantity(quantity)
                .createdAt(TEST_TIME)
                .updatedAt(TEST_TIME)
                .build();

        cart.getItems().add(item);
        cartRepository.save(cart);
    }

    private OrderJpaEntity seedStockReservedOrder(UserEntity user) {
        OrderJpaEntity order = TestFixtures.order(user, UUID.randomUUID());
        order.setStatus(OrderStatus.STOCK_RESERVED);
        return orderRepository.save(order);
    }

    private InitiateVnPayPaymentCommand initiatePaymentCommand(UUID userId,
                                                               UUID orderId,
                                                               String idempotencyKey) {
        return new InitiateVnPayPaymentCommand(
                userId,
                orderId,
                CLIENT_IP,
                idempotencyKey
        );
    }

    private List<OrderJpaEntity> ordersOf(UUID userId) {
        return orderRepository.findByUserId(userId, PageRequest.of(0, 20)).getContent();
    }

    private List<PaymentJpaEntity> paymentsForOrder(UUID orderId) {
        return paymentRepository.findAll().stream()
                .filter(payment -> orderId.equals(payment.getOrderId()))
                .toList();
    }

    private List<PaymentAttemptJpaEntity> paymentAttemptsForPayment(UUID paymentId) {
        return paymentAttemptRepository.findAll().stream()
                .filter(attempt -> paymentId.equals(attempt.getPaymentId()))
                .toList();
    }

    private OrderJpaEntity reloadOrder(OrderJpaEntity order) {
        return orderRepository.findById(order.getId()).orElseThrow();
    }

    private void assertCompletedIdempotencyKey(UUID userId, String idempotencyKey) {
        assertThat(idempotencyKeyOf(userId, idempotencyKey).getStatus())
                .isEqualTo(IdempotencyStatus.COMPLETED);
    }

    private IdempotencyKeyEntity idempotencyKeyOf(UUID userId, String idempotencyKey) {
        List<IdempotencyKeyEntity> keys = idempotencyKeysOf(userId, idempotencyKey);

        assertThat(keys).hasSize(1);
        return keys.getFirst();
    }

    private List<IdempotencyKeyEntity> idempotencyKeysOf(UUID userId, String idempotencyKey) {
        return idempotencyKeyRepository.findAll().stream()
                .filter(key -> userId.equals(key.getUserId()))
                .filter(key -> idempotencyKey.equals(key.getIdempotencyKey()))
                .toList();
    }

    private record PlaceOrderScenario(
            UserEntity user,
            ProductEntity product
    ) {
    }

}
