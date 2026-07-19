package com.techora.order;

import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.order.application.service.payment.OrderPaymentWindowExpirationService;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
import com.techora.payment.domain.valueobject.PaymentStatus;
import com.techora.payment.infra.persistence.PaymentJpaEntity;
import com.techora.payment.infra.persistence.PaymentJpaRepository;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import com.techora.user.entity.UserEntity;
import com.techora.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPaymentWindowExpirationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OrderPaymentWindowExpirationService expirationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void expiredUnpaidOrderCancelsOrderExpiresPaymentAndReleasesReservedInventory() {
        ExpiredReservationScenario scenario = seedExpiredPaymentPendingOrderWithReservedInventory();

        int expiredOrders = expirationService.expireUnpaidOrders();
        int secondRunExpiredOrders = expirationService.expireUnpaidOrders();

        OrderJpaEntity order = orderRepository.findById(scenario.order().getId()).orElseThrow();
        PaymentJpaEntity payment = paymentRepository.findById(scenario.payment().getId()).orElseThrow();
        InventoryItemEntity stock = inventoryItemRepository.findByProductId(scenario.productId()).orElseThrow();
        List<InventoryReservationEntity> reservations =
                reservationRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());

        assertThat(expiredOrders).isEqualTo(1);
        assertThat(secondRunExpiredOrders).isZero();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(stock.getQuantityOnHand()).isEqualTo(1);
        assertThat(stock.getReservedQuantity()).isZero();
        assertThat(reservations)
                .singleElement()
                .satisfies(reservation -> {
                    assertThat(reservation.getProductId()).isEqualTo(scenario.productId());
                    assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
                });
    }

    private ExpiredReservationScenario seedExpiredPaymentPendingOrderWithReservedInventory() {
        Instant deadline = Instant.now().minusSeconds(60);
        UUID productId = UUID.randomUUID();
        UserEntity user = userRepository.save(TestFixtures.user());

        OrderJpaEntity order = TestFixtures.order(user, productId);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentDeadlineAt(deadline);
        order = orderRepository.save(order);

        PaymentJpaEntity payment = TestFixtures.payment(order.getId(), user.getId());
        payment.setUsername(user.getUsername());
        payment.setOrderPaymentDeadlineAt(deadline);
        payment = paymentRepository.save(payment);

        InventoryItemEntity stock = TestFixtures.inventoryItem(productId, 1);
        stock.reserve(1);
        inventoryItemRepository.save(stock);
        reservationRepository.save(TestFixtures.reservedInventoryReservation(
                order.getId(),
                productId,
                1,
                deadline
        ));

        return new ExpiredReservationScenario(order, payment, productId);
    }

    private record ExpiredReservationScenario(
            OrderJpaEntity order,
            PaymentJpaEntity payment,
            UUID productId
    ) {
    }
}
