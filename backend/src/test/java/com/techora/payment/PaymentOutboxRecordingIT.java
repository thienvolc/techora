package com.techora.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.payment.application.command.HandleVnPayIpnCommand;
import com.techora.payment.application.usecase.HandleVnPayIpnUseCase;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentOutboxRecordingIT extends AbstractIntegrationTest {

    private static final String PAYMENT_EVENTS_TOPIC = "techora.payment.events";

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
    private ObjectMapper objectMapper;

    @Test
    void confirmedPaymentEventRecordsOutboxEnvelopeAndScalarPayload() throws Exception {
        PaymentScenario scenario = seedPendingPaymentScenario();

        useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        OutboxEventEntity outboxEvent = outboxRowsForPayment(scenario.payment().getId()).getFirst();

        assertOutboxMetadata(outboxEvent, scenario, OutboxEventType.PAYMENT_CONFIRMED);
        assertOutboxHeaders(outboxEvent, OutboxEventType.PAYMENT_CONFIRMED);
        assertOutboxEnvelope(outboxEvent, scenario, OutboxEventType.PAYMENT_CONFIRMED);
    }

    @Test
    void failedPaymentEventRecordsOutboxEnvelopeAndScalarPayload() throws Exception {
        PaymentScenario scenario = seedPendingPaymentScenario();

        useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.failed(
                        scenario.attempt().getProviderReference(),
                        scenario.attempt().getAmount()
                )
        ));

        OutboxEventEntity outboxEvent = outboxRowsForPayment(scenario.payment().getId()).getFirst();

        assertOutboxMetadata(outboxEvent, scenario, OutboxEventType.PAYMENT_FAILED);
        assertOutboxHeaders(outboxEvent, OutboxEventType.PAYMENT_FAILED);
        assertOutboxEnvelope(outboxEvent, scenario, OutboxEventType.PAYMENT_FAILED);
    }

    @Test
    void reconciliationRequiredPaymentEventRecordsOutboxEnvelopeAndScalarPayload() throws Exception {
        PaymentScenario scenario = seedPendingPaymentScenario();
        BigDecimal mismatchedProviderAmount = BigDecimal.valueOf(99_00, 2);

        useCase.execute(new HandleVnPayIpnCommand(
                VnPayIpnTestParams.successful(
                        scenario.attempt().getProviderReference(),
                        mismatchedProviderAmount
                )
        ));

        OutboxEventEntity outboxEvent = outboxRowsForPayment(scenario.payment().getId()).getFirst();

        assertOutboxMetadata(outboxEvent, scenario, OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED);
        assertOutboxHeaders(outboxEvent, OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED);
        assertOutboxEnvelope(
                outboxEvent,
                scenario,
                OutboxEventType.PAYMENT_RECONCILIATION_REQUIRED,
                PaymentReconciliationReason.AMOUNT_MISMATCHED
        );
    }

    private PaymentScenario seedPendingPaymentScenario() {
        Instant deadline = Instant.now().plusSeconds(3600);
        UUID productId = UUID.randomUUID();

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

    private void assertOutboxMetadata(OutboxEventEntity outboxEvent,
                                      PaymentScenario scenario,
                                      OutboxEventType expectedEventType) {
        assertThat(outboxEvent.getId()).isNotNull();
        assertThat(outboxEvent.getEventId()).isEqualTo(outboxEvent.getId());
        assertThat(outboxEvent.getAggregateType()).isEqualTo(OutboxAggregateType.PAYMENT);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(scenario.payment().getId());
        assertThat(outboxEvent.getEventType()).isEqualTo(expectedEventType);
        assertThat(outboxEvent.getTopic()).isEqualTo(PAYMENT_EVENTS_TOPIC);
        assertThat(outboxEvent.getMessageKey()).isEqualTo(scenario.order().getId().toString());
        assertThat(outboxEvent.getEventVersion()).isEqualTo(1);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(outboxEvent.getRetryCount()).isZero();
        assertThat(outboxEvent.getNextAttemptAt()).isNotNull();
    }

    private void assertOutboxHeaders(OutboxEventEntity outboxEvent, OutboxEventType expectedEventType) throws Exception {
        JsonNode headers = objectMapper.readTree(outboxEvent.getHeaders());

        assertThat(headers.get("source").asText()).isEqualTo("payment");
        assertThat(headers.get("eventId").asText()).isEqualTo(outboxEvent.getEventId().toString());
        assertThat(headers.get("eventType").asText()).isEqualTo(expectedEventType.name());
        assertThat(headers.get("eventVersion").asText()).isEqualTo("1");
    }

    private void assertOutboxEnvelope(OutboxEventEntity outboxEvent,
                                      PaymentScenario scenario,
                                      OutboxEventType expectedEventType) throws Exception {
        assertOutboxEnvelope(outboxEvent, scenario, expectedEventType, null);
    }

    private void assertOutboxEnvelope(OutboxEventEntity outboxEvent,
                                      PaymentScenario scenario,
                                      OutboxEventType expectedEventType,
                                      PaymentReconciliationReason expectedReason) throws Exception {
        JsonNode envelope = objectMapper.readTree(outboxEvent.getPayload());

        assertThat(envelope.get("eventId").asText()).isEqualTo(outboxEvent.getEventId().toString());
        assertThat(envelope.get("eventType").asText()).isEqualTo(expectedEventType.name());
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("occurredAt").isNull()).isFalse();

        JsonNode data = envelope.get("data");
        assertThat(fieldNames(data))
                .containsExactlyInAnyOrderElementsOf(expectedPayloadFields(expectedReason));
        assertThat(data.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(data.get("paymentId").asText()).isEqualTo(scenario.payment().getId().toString());
        assertThat(data.get("attemptId").asText()).isEqualTo(scenario.attempt().getId().toString());
        assertThat(data.get("orderId").asText()).isEqualTo(scenario.order().getId().toString());
        assertThat(data.get("userId").asText()).isEqualTo(scenario.order().getUser().getId().toString());
        assertThat(data.get("providerName").asText()).isEqualTo(PaymentProvider.VNPAY.name());
        assertThat(data.get("providerReference").asText()).isEqualTo(scenario.attempt().getProviderReference());
        assertThat(data.get("amount").decimalValue()).isEqualByComparingTo(scenario.payment().getAmount());
        assertThat(data.get("occurredAt").asText()).isEqualTo(envelope.get("occurredAt").asText());
        if (expectedReason != null) {
            assertThat(data.get("reason").asText()).isEqualTo(expectedReason.name());
        }
    }

    private List<String> expectedPayloadFields(PaymentReconciliationReason expectedReason) {
        List<String> fields = new ArrayList<>(List.of(
                "eventVersion",
                "paymentId",
                "attemptId",
                "orderId",
                "userId",
                "providerName",
                "providerReference",
                "amount",
                "occurredAt"
        ));
        if (expectedReason != null) {
            fields.add("reason");
        }
        return fields;
    }

    private List<String> fieldNames(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        return fields;
    }

    private List<OutboxEventEntity> outboxRowsForPayment(UUID paymentId) {
        List<OutboxEventEntity> outboxRows = outboxRepository.findAll().stream()
                .filter(event -> paymentId.equals(event.getAggregateId()))
                .toList();

        assertThat(outboxRows).singleElement();
        return outboxRows;
    }

    private record PaymentScenario(
            OrderJpaEntity order,
            PaymentJpaEntity payment,
            PaymentAttemptJpaEntity attempt
    ) {
    }
}
