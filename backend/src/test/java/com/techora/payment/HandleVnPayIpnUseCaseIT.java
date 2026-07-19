package com.techora.payment;

import com.techora.order.domain.entity.OrderStatus;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.payment.application.command.HandleVnPayIpnCommand;
import com.techora.payment.application.model.VnPayIpnReply;
import com.techora.payment.application.usecase.HandleVnPayIpnUseCase;
import com.techora.payment.domain.valueobject.PaymentAttemptStatus;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
import com.techora.payment.domain.valueobject.PaymentStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HandleVnPayIpnUseCaseIT extends AbstractIntegrationTest {

    @Autowired
    private HandleVnPayIpnUseCase useCase;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private PaymentAttemptJpaRepository attemptRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Test
    void invalidSignatureDoesNotMutatePaymentAttemptOrOrder() {
        PaymentScenario scenario = seedPendingPaymentScenario();

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successfulWithInvalidSignature(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        PaymentJpaEntity payment = paymentRepository.findById(scenario.payment().getId()).orElseThrow();
        PaymentAttemptJpaEntity attempt = attemptRepository.findById(scenario.attempt().getId()).orElseThrow();
        OrderJpaEntity order = orderRepository.findById(scenario.order().getId()).orElseThrow();

        assertThat(reply.responseCode()).isEqualTo("97");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
        assertThat(attempt.getProviderResultReceivedAt()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void unknownTxnRefReturnsNotFoundAndDoesNotMutateExistingPayment() {
        PaymentScenario scenario = seedPendingPaymentScenario();
        long outboxRowsBeforeIpn = countOutboxRowsForPayment(scenario.payment().getId());
        String unknownTxnRef = "UNKNOWN-" + UUID.randomUUID();

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        unknownTxnRef,
                        scenario.attempt().getAmount()
                )
        ));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);

        assertThat(reply.responseCode()).isEqualTo("01");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PENDING);
        assertThat(attempt.getProviderResultReceivedAt()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(countOutboxRowsForPayment(scenario.payment().getId()))
                .isEqualTo(outboxRowsBeforeIpn)
                .isZero();
    }

    @Test
    void successfulIpnConfirmsPaymentAttemptPaymentAndOrder() {
        PaymentScenario scenario = seedPendingPaymentScenario();

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        PaymentJpaEntity payment = paymentRepository.findById(scenario.payment().getId()).orElseThrow();
        PaymentAttemptJpaEntity attempt = attemptRepository.findById(scenario.attempt().getId()).orElseThrow();
        OrderJpaEntity order = orderRepository.findById(scenario.order().getId()).orElseThrow();

        assertThat(reply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PAID);
        assertProviderEvidenceStored(attempt, "00", "00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void successfulIpnConfirmsReservedInventoryAfterOrderIsPaid() {
        ReservedPaymentScenario scenario = seedPendingPaymentScenarioWithReservedInventory();

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.payment().attempt().getProviderReference(),
                        scenario.payment().attempt().getAmount()
                )
        ));

        OrderJpaEntity order = reloadOrder(scenario.payment());
        InventoryItemEntity stock = inventoryItemRepository.findByProductId(scenario.productId()).orElseThrow();
        List<InventoryReservationEntity> reservations =
                reservationRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());

        assertThat(reply.responseCode()).isEqualTo("00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(stock.getQuantityOnHand()).isZero();
        assertThat(stock.getReservedQuantity()).isZero();
        assertThat(reservations)
                .singleElement()
                .satisfies(reservation -> {
                    assertThat(reservation.getProductId()).isEqualTo(scenario.productId());
                    assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.CONFIRMED);
                });
    }

    @Test
    void successfulIpnRecordsPaymentConfirmedOutboxEvent() {
        PaymentScenario scenario = seedPendingPaymentScenario();

        useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        assertThat(outboxRowsForPayment(scenario.payment().getId()))
                .singleElement()
                .extracting(OutboxEventEntity::getEventType)
                .isEqualTo(OutboxEventType.PAYMENT_CONFIRMED);
    }

    @Test
    void providerFailedIpnMarksAttemptFailedAndKeepsOrderPaymentPending() {
        PaymentScenario scenario = seedPendingPaymentScenario();

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.failed(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);
        List<OutboxEventEntity> outboxRows = outboxRowsForPayment(scenario.payment().getId());

        assertThat(reply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertProviderEvidenceStored(attempt, "24", "02");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(outboxRows)
                .singleElement()
                .extracting(OutboxEventEntity::getEventType)
                .isEqualTo(OutboxEventType.PAYMENT_FAILED);
    }

    @Test
    void duplicateFailedIpnIsIdempotentAndDoesNotAppendOutboxTwice() {
        PaymentScenario scenario = seedPendingPaymentScenario();

        var params = VnPayIpnTestParams.failed(
                scenario.attempt().getProviderReference(),
                scenario.attempt().getAmount()
        );

        VnPayIpnReply firstReply = useCase.execute(new HandleVnPayIpnCommand(params));
        PaymentAttemptJpaEntity firstHandledAttempt = reloadAttempt(scenario);
        long outboxRowsAfterFirstIpn = countOutboxRowsForPayment(scenario.payment().getId());

        VnPayIpnReply duplicateReply = useCase.execute(new HandleVnPayIpnCommand(params));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);

        assertThat(firstReply.responseCode()).isEqualTo("00");
        assertThat(duplicateReply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(attempt.getProviderResultReceivedAt())
                .isEqualTo(firstHandledAttempt.getProviderResultReceivedAt());
        assertThat(countOutboxRowsForPayment(scenario.payment().getId()))
                .isEqualTo(outboxRowsAfterFirstIpn)
                .isEqualTo(1);
    }

    @Test
    void duplicateSuccessIpnIsIdempotentAndDoesNotAppendOutboxTwice() {
        PaymentScenario scenario = seedPendingPaymentScenario();

        var params = VnPayIpnTestParams.successful(
                scenario.attempt().getProviderReference(),
                scenario.attempt().getAmount()
        );

        VnPayIpnReply firstReply = useCase.execute(new HandleVnPayIpnCommand(params));
        PaymentAttemptJpaEntity firstHandledAttempt = reloadAttempt(scenario);
        long outboxRowsAfterFirstIpn = countOutboxRowsForPayment(scenario.payment().getId());

        VnPayIpnReply duplicateReply = useCase.execute(new HandleVnPayIpnCommand(params));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);

        assertThat(firstReply.responseCode()).isEqualTo("00");
        assertThat(duplicateReply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(attempt.getProviderResultReceivedAt())
                .isEqualTo(firstHandledAttempt.getProviderResultReceivedAt());
        assertThat(countOutboxRowsForPayment(scenario.payment().getId()))
                .isEqualTo(outboxRowsAfterFirstIpn)
                .isEqualTo(1);
    }

    @Test
    void amountMismatchMovesPaymentAndAttemptToReconciliation() {
        PaymentScenario scenario = seedPendingPaymentScenario();
        BigDecimal mismatchedProviderAmount = BigDecimal.valueOf(99_00, 2);

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        mismatchedProviderAmount
                )
        ));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);

        assertThat(reply.responseCode()).isEqualTo("04");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
        assertThat(attempt.getReconciliationReason()).isEqualTo(PaymentReconciliationReason.AMOUNT_MISMATCHED);
        assertProviderEvidenceStored(attempt, "00", "00");
        assertThat(attempt.getRawProviderPayload()).contains("vnp_Amount=9900");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(countOutboxRowsForPayment(scenario.payment().getId())).isEqualTo(1);
    }

    @Test
    void duplicateAmountMismatchIpnDoesNotAppendReconciliationOutboxTwice() {
        PaymentScenario scenario = seedPendingPaymentScenario();
        BigDecimal mismatchedProviderAmount = BigDecimal.valueOf(99_00, 2);
        var params = VnPayIpnTestParams.successful(
                scenario.attempt().getProviderReference(),
                mismatchedProviderAmount
        );

        VnPayIpnReply firstReply = useCase.execute(new HandleVnPayIpnCommand(params));
        PaymentAttemptJpaEntity firstHandledAttempt = reloadAttempt(scenario);
        long outboxRowsAfterFirstIpn = countOutboxRowsForPayment(scenario.payment().getId());

        VnPayIpnReply duplicateReply = useCase.execute(new HandleVnPayIpnCommand(params));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);
        List<OutboxEventEntity> outboxRows = outboxRowsForPayment(scenario.payment().getId());

        assertThat(firstReply.responseCode()).isEqualTo("04");
        assertThat(duplicateReply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(attempt.getProviderResultReceivedAt())
                .isEqualTo(firstHandledAttempt.getProviderResultReceivedAt());
        assertThat(countOutboxRowsForPayment(scenario.payment().getId()))
                .isEqualTo(outboxRowsAfterFirstIpn)
                .isEqualTo(1);
        assertThat(outboxRows)
                .singleElement()
                .extracting(OutboxEventEntity::getEventType)
                .isEqualTo(OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED);
    }

    @Test
    void lateSuccessIpnMovesPaymentAndAttemptToReconciliation() {
        Instant expiredDeadline = Instant.now().minusSeconds(60);
        PaymentScenario scenario = seedPendingPaymentScenario(expiredDeadline);

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);

        assertThat(reply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
        assertProviderEvidenceStored(attempt, "00", "00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertReconciliationReason(scenario, PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED);
    }

    @Test
    void successIpnForExpiredAttemptRequiresReconciliationWithLateExpiredReason() {
        PaymentScenario scenario = seedPendingPaymentScenario();
        PaymentAttemptJpaEntity expiredAttempt = reloadAttempt(scenario);
        expiredAttempt.setStatus(PaymentAttemptStatus.EXPIRED);
        expiredAttempt.setExpiredAt(Instant.now().minusSeconds(60));
        attemptRepository.save(expiredAttempt);

        VnPayIpnReply reply = useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        PaymentJpaEntity payment = reloadPayment(scenario);
        PaymentAttemptJpaEntity attempt = reloadAttempt(scenario);
        OrderJpaEntity order = reloadOrder(scenario);

        assertThat(reply.responseCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECONCILIATION_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.RECONCILIATION_REQUIRED);
        assertProviderEvidenceStored(attempt, "00", "00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertReconciliationReason(scenario, PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED);
    }

    private PaymentScenario seedPendingPaymentScenario() {
        return seedPendingPaymentScenario(Instant.now().plusSeconds(3600));
    }

    private PaymentScenario seedPendingPaymentScenario(Instant deadline) {
        return seedPendingPaymentScenario(deadline, UUID.randomUUID());
    }

    private ReservedPaymentScenario seedPendingPaymentScenarioWithReservedInventory() {
        Instant deadline = Instant.now().plusSeconds(3600);
        UUID productId = UUID.randomUUID();
        PaymentScenario scenario = seedPendingPaymentScenario(deadline, productId);

        InventoryItemEntity stock = TestFixtures.inventoryItem(productId, 1);
        stock.reserve(1);
        inventoryItemRepository.save(stock);
        reservationRepository.save(TestFixtures.reservedInventoryReservation(
                scenario.order().getId(),
                productId,
                1,
                deadline
        ));

        return new ReservedPaymentScenario(scenario, productId);
    }

    private PaymentScenario seedPendingPaymentScenario(Instant deadline, UUID productId) {
        UserEntity user = userRepository.save(TestFixtures.user());
        OrderJpaEntity order = TestFixtures.order(user, productId);
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentDeadlineAt(deadline);
        order = orderRepository.save(order);

        PaymentJpaEntity payment = TestFixtures.payment(order.getId(), user.getId());
        payment.setUsername(user.getUsername());
        payment.setAmount(BigDecimal.valueOf(100_00, 2));
        payment.setOrderPaymentDeadlineAt(deadline);
        payment = paymentRepository.save(payment);

        PaymentAttemptJpaEntity attempt = TestFixtures.paymentAttempt(payment.getId(), order.getId(), user.getId());
        attempt.setAmount(payment.getAmount());
        attempt.setExpiresAt(deadline);
        attempt = attemptRepository.save(attempt);

        return new PaymentScenario(order, payment, attempt);
    }

    private PaymentJpaEntity reloadPayment(PaymentScenario scenario) {
        return paymentRepository.findById(scenario.payment().getId()).orElseThrow();
    }

    private PaymentAttemptJpaEntity reloadAttempt(PaymentScenario scenario) {
        return attemptRepository.findById(scenario.attempt().getId()).orElseThrow();
    }

    private OrderJpaEntity reloadOrder(PaymentScenario scenario) {
        return orderRepository.findById(scenario.order().getId()).orElseThrow();
    }

    private void assertProviderEvidenceStored(PaymentAttemptJpaEntity attempt,
                                              String responseCode,
                                              String statusCode) {
        assertThat(attempt.getProviderResponseCode()).isEqualTo(responseCode);
        assertThat(attempt.getProviderStatusCode()).isEqualTo(statusCode);
        assertThat(attempt.getProviderTransactionId()).isEqualTo("14123456");
        assertThat(attempt.getProviderResultReceivedAt()).isNotNull();
        assertThat(attempt.getRawProviderPayload())
                .contains("vnp_ResponseCode=" + responseCode)
                .contains("vnp_TransactionStatus=" + statusCode)
                .contains("vnp_TransactionNo=14123456");
    }

    private long countOutboxRowsForPayment(UUID paymentId) {
        return outboxRowsForPayment(paymentId).size();
    }

    private List<OutboxEventEntity> outboxRowsForPayment(UUID paymentId) {
        return outboxRepository.findAll().stream()
                .filter(event -> paymentId.equals(event.getAggregateId()))
                .toList();
    }

    private void assertReconciliationReason(PaymentScenario scenario, PaymentReconciliationReason reason) {
        assertThat(reloadAttempt(scenario).getReconciliationReason()).isEqualTo(reason);
        assertThat(outboxRowsForPayment(scenario.payment().getId()))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getEventType()).isEqualTo(OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED);
                    assertThat(event.getPayload()).contains("\"reason\":\"" + reason.name() + "\"");
                });
    }

    private record PaymentScenario(
            OrderJpaEntity order,
            PaymentJpaEntity payment,
            PaymentAttemptJpaEntity attempt
    ) {
    }

    private record ReservedPaymentScenario(
            PaymentScenario payment,
            UUID productId
    ) {
    }
}
