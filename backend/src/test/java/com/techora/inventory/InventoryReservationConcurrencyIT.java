package com.techora.inventory;

import com.techora.cart.entity.CartEntity;
import com.techora.cart.entity.CartItemEntity;
import com.techora.cart.repository.CartRepository;
import com.techora.catalog.domain.entity.CategoryEntity;
import com.techora.catalog.domain.entity.ProductEntity;
import com.techora.catalog.persistence.repository.CategoryRepository;
import com.techora.catalog.persistence.repository.ProductRepository;
import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.model.PlaceOrderView;
import com.techora.order.application.usecase.PlaceOrderUseCase;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
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

class InventoryReservationConcurrencyIT extends AbstractIntegrationTest {

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

    @Test
    void concurrentCheckoutForSameProductDoesNotOversellReservedStock() throws Exception {
        CheckoutRaceScenario scenario = seedTwoCartsForSameProduct(1, 1);

        List<CheckoutAttempt> attempts = ConcurrentTestRunner.run(
                2,
                index -> () -> checkoutAttempt(scenario.command(index)));

        List<CheckoutAttempt> successfulAttempts = attempts.stream()
                .filter(CheckoutAttempt::isSuccess)
                .toList();
        List<CheckoutAttempt> businessErrors = attempts.stream()
                .filter(CheckoutAttempt::isBusinessError)
                .toList();
        List<CheckoutAttempt> failedAttempts = attempts.stream()
                .filter(CheckoutAttempt::isFailure)
                .toList();

        assertThat(failedAttempts).isEmpty();
        assertThat(successfulAttempts).hasSize(1);
        assertThat(businessErrors)
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.responseCode())
                        .isEqualTo(ResponseCode.INSUFFICIENT_STOCK));

        PlaceOrderView placedOrder = successfulAttempts.getFirst().result();
        InventoryItemEntity stock = inventoryItemRepository.findByProductId(scenario.product().getId()).orElseThrow();

        assertThat(stock.getQuantityOnHand()).isEqualTo(1);
        assertThat(stock.getReservedQuantity()).isEqualTo(1);
        assertThat(reservationsForProduct(scenario.product().getId()))
                .singleElement()
                .satisfies(reservation -> {
                    assertThat(reservation.getOrderId()).isEqualTo(placedOrder.id());
                    assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RESERVED);
                });
        assertThat(ordersForUsers(scenario.users())).hasSize(1);
        assertThat(paymentsForOrder(placedOrder.id())).hasSize(1);
        assertThat(paymentAttemptsForPayment(placedOrder.paymentId())).hasSize(1);
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

    private CheckoutRaceScenario seedTwoCartsForSameProduct(int cartQuantity, int stockQuantity) {
        CategoryEntity category = categoryRepository.save(TestFixtures.category());
        ProductEntity product = productRepository.save(TestFixtures.product(category));
        inventoryItemRepository.save(TestFixtures.inventoryItem(product.getId(), stockQuantity));

        UserEntity firstUser = seedUserCart(product, cartQuantity);
        UserEntity secondUser = seedUserCart(product, cartQuantity);

        return new CheckoutRaceScenario(
                product,
                List.of(firstUser, secondUser),
                List.of(placeOrderCommand(firstUser), placeOrderCommand(secondUser))
        );
    }

    private UserEntity seedUserCart(ProductEntity product, int quantity) {
        UserEntity user = userRepository.save(TestFixtures.user());
        seedCart(user, product, quantity);
        return user;
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

    private PlaceOrderCommand placeOrderCommand(UserEntity user) {
        return new PlaceOrderCommand(
                user.getId(),
                CLIENT_IP,
                "checkout-" + UUID.randomUUID()
        );
    }

    private List<InventoryReservationEntity> reservationsForProduct(UUID productId) {
        return reservationRepository.findAll().stream()
                .filter(reservation -> productId.equals(reservation.getProductId()))
                .toList();
    }

    private List<OrderJpaEntity> ordersForUsers(List<UserEntity> users) {
        return users.stream()
                .flatMap(user -> orderRepository.findByUserId(user.getId(), PageRequest.of(0, 20)).stream())
                .toList();
    }

    private List<PaymentJpaEntity> paymentsForOrder(UUID orderId) {
        return paymentRepository.findAll().stream()
                .filter(payment -> orderId.equals(payment.getOrderId()))
                .toList();
    }

    private List<?> paymentAttemptsForPayment(UUID paymentId) {
        return paymentAttemptRepository.findAll().stream()
                .filter(attempt -> paymentId.equals(attempt.getPaymentId()))
                .toList();
    }

    private record CheckoutRaceScenario(
            ProductEntity product,
            List<UserEntity> users,
            List<PlaceOrderCommand> commands
    ) {

        PlaceOrderCommand command(int index) {
            return commands.get(index);
        }
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
