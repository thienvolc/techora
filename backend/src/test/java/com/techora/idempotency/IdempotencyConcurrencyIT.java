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
import com.techora.idempotency.entity.IdempotencyStatus;
import com.techora.idempotency.repository.IdempotencyKeyRepository;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.model.PlaceOrderView;
import com.techora.order.application.usecase.PlaceOrderUseCase;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
import com.techora.payment.infra.persistence.PaymentAttemptJpaEntity;
import com.techora.payment.infra.persistence.PaymentAttemptJpaRepository;
import com.techora.payment.infra.persistence.PaymentJpaEntity;
import com.techora.payment.infra.persistence.PaymentJpaRepository;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.ConcurrentTestRunner;
import com.techora.testsupport.TestFixtures;
import com.techora.user.entity.UserEntity;
import com.techora.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyConcurrencyIT extends AbstractIntegrationTest {

    private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final String CLIENT_IP = "127.0.0.1";

    @Autowired
    private PlaceOrderUseCase placeOrderUseCase;

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
    void concurrentSameIdempotencyKeyCheckoutCreatesOnlyOneOrderPaymentFlow() throws Exception {
        PlaceOrderScenario scenario = seedPlaceOrderScenario(2, 10);
        String idempotencyKey = "checkout-" + UUID.randomUUID();
        PlaceOrderCommand command = new PlaceOrderCommand(
                scenario.user().getId(),
                CLIENT_IP,
                idempotencyKey
        );

        List<CheckoutAttempt> attempts = ConcurrentTestRunner.run(
                2,
                () -> checkoutAttempt(command));

        List<CheckoutAttempt> successfulAttempts = attempts.stream()
                .filter(CheckoutAttempt::isSuccess)
                .toList();
        List<CheckoutAttempt> failedAttempts = attempts.stream()
                .filter(CheckoutAttempt::isFailure)
                .toList();
        List<CheckoutAttempt> businessErrors = attempts.stream()
                .filter(CheckoutAttempt::isBusinessError)
                .toList();

        assertThat(failedAttempts).isEmpty();
        assertThat(successfulAttempts).isNotEmpty();
        assertThat(businessErrors)
                .allSatisfy(attempt -> assertThat(attempt.responseCode())
                        .isEqualTo(ResponseCode.IDEMPOTENCY_REQUEST_PROCESSING));

        PlaceOrderView placedOrder = successfulAttempts.getFirst().result();
        successfulAttempts.forEach(attempt -> assertThat(attempt.result()).isEqualTo(placedOrder));

        assertThat(ordersOf(scenario.user().getId())).hasSize(1);
        assertThat(paymentsForOrder(placedOrder.id())).hasSize(1);
        assertThat(paymentAttemptsForPayment(placedOrder.paymentId())).hasSize(1);
        assertThat(reservationRepository.findByOrderIdOrderByCreatedAtAsc(placedOrder.id())).hasSize(1);
        assertCompletedIdempotencyKey(scenario.user().getId(), idempotencyKey);
    }

    private CheckoutAttempt checkoutAttempt(PlaceOrderCommand command) {
        try {
            return CheckoutAttempt.success(placeOrderUseCase.execute(command));
        } catch (BusinessException ex) {
            return CheckoutAttempt.businessError(ex.getResponseCode());
        } catch (RuntimeException ex) {
            return CheckoutAttempt.failure(ex);
        }
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

    private void assertCompletedIdempotencyKey(UUID userId, String idempotencyKey) {
        assertThat(idempotencyKeyRepository.findAll().stream()
                .filter(key -> userId.equals(key.getUserId()))
                .filter(key -> idempotencyKey.equals(key.getIdempotencyKey()))
                .toList())
                .singleElement()
                .satisfies(key -> assertThat(key.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED));
    }

    private record PlaceOrderScenario(
            UserEntity user,
            ProductEntity product
    ) {
    }

    private record CheckoutAttempt(
            PlaceOrderView result,
            ResponseCode responseCode,
            Throwable failure
    ) {

        static CheckoutAttempt success(PlaceOrderView result) {
            return new CheckoutAttempt(result, null, null);
        }

        static CheckoutAttempt businessError(ResponseCode responseCode) {
            return new CheckoutAttempt(null, responseCode, null);
        }

        static CheckoutAttempt failure(Throwable failure) {
            return new CheckoutAttempt(null, null, failure);
        }

        boolean isSuccess() {
            return result != null;
        }

        boolean isBusinessError() {
            return responseCode != null;
        }

        boolean isFailure() {
            return failure != null;
        }
    }
}
