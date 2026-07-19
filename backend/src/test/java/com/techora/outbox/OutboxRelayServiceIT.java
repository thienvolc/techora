package com.techora.outbox;

import com.techora.common.infra.config.prop.OutboxRetryProperties;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.order.infra.persistence.OrderJpaEntity;
import com.techora.order.infra.persistence.OrderJpaRepository;
import com.techora.orderhistory.repository.OrderHistoryRepository;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxMessage;
import com.techora.outbox.dto.RelaySummary;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.publisher.OutboxMessagePublisher;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.outbox.service.OutboxRelayService;
import com.techora.payment.domain.valueobject.PaymentStatus;
import com.techora.payment.infra.persistence.PaymentJpaEntity;
import com.techora.payment.infra.persistence.PaymentJpaRepository;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import com.techora.user.entity.UserEntity;
import com.techora.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxRelayServiceIT extends AbstractIntegrationTest {

    private static final List<OutboxEventType> PAYMENT_CONFIRMED_EVENTS =
            List.of(OutboxEventType.PAYMENT_CONFIRMED);

    @Autowired
    private OutboxRelayService outboxRelayService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxRetryProperties retryProperties;

    @Autowired
    private CapturingOutboxMessagePublisher messagePublisher;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        messagePublisher.clear();
    }

    @Test
    void successfulPublishMarksClaimedRowsPublished() {
        List<OutboxEventEntity> readyEvents = seedReadyPaymentEvents(2);

        RelaySummary summary = outboxRelayService.relayPendingEvents(PAYMENT_CONFIRMED_EVENTS, 10);

        assertThat(summary.attempted()).isEqualTo(2);
        assertThat(summary.succeeded()).isEqualTo(2);
        assertThat(summary.failed()).isZero();
        assertPublishedMessages(readyEvents);
        assertRowsPublished(readyEvents);
    }

    @Test
    void publishFailureSchedulesRetryWithBackoff() {
        OutboxEventEntity readyEvent = seedReadyPaymentEvents(1).getFirst();
        messagePublisher.failWith(new IllegalStateException("Kafka unavailable"));
        Instant beforeRelay = Instant.now();

        RelaySummary summary = outboxRelayService.relayPendingEvents(PAYMENT_CONFIRMED_EVENTS, 10);

        assertThat(summary.attempted()).isEqualTo(1);
        assertThat(summary.succeeded()).isZero();
        assertThat(summary.failed()).isEqualTo(1);
        assertPublishedMessages(List.of(readyEvent));
        assertRowScheduledForRetry(readyEvent, beforeRelay);
    }

    @Test
    void retryExhaustionMarksRowFailed() {
        OutboxEventEntity exhaustedEvent = seedReadyPaymentEventWithRetryCount(retryProperties.maxRetries());
        messagePublisher.failWith(new IllegalStateException("Kafka still unavailable"));

        RelaySummary summary = outboxRelayService.relayPendingEvents(PAYMENT_CONFIRMED_EVENTS, 10);

        assertThat(summary.attempted()).isEqualTo(1);
        assertThat(summary.succeeded()).isZero();
        assertThat(summary.failed()).isEqualTo(1);
        assertPublishedMessages(List.of(exhaustedEvent));
        assertRowMarkedFailed(exhaustedEvent);
    }

    @Test
    void relayDoesNotExecutePaymentOrOrderBusinessHandler() {
        RelayBoundaryScenario scenario = seedPendingPaymentWithConfirmedOutboxEvent();

        RelaySummary summary = outboxRelayService.relayPendingEvents(PAYMENT_CONFIRMED_EVENTS, 10);

        assertThat(summary.attempted()).isEqualTo(1);
        assertThat(summary.succeeded()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        assertPublishedMessages(List.of(scenario.outboxEvent()));
        assertRowsPublished(List.of(scenario.outboxEvent()));
        assertBusinessStateUnchanged(scenario);
    }

    private List<OutboxEventEntity> seedReadyPaymentEvents(int count) {
        Instant now = Instant.now();
        List<OutboxEventEntity> events = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    return newReadyPaymentEvent(now, index);
                })
                .toList();

        return outboxEventRepository.saveAll(events);
    }

    private OutboxEventEntity seedReadyPaymentEventWithRetryCount(int retryCount) {
        OutboxEventEntity event = newReadyPaymentEvent(Instant.now(), 0);
        event.setRetryCount(retryCount);

        return outboxEventRepository.save(event);
    }

    private RelayBoundaryScenario seedPendingPaymentWithConfirmedOutboxEvent() {
        Instant deadline = Instant.now().plusSeconds(3600);
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

        OutboxEventEntity outboxEvent = newReadyPaymentEvent(payment.getId(), order.getId(), Instant.now());
        outboxEvent = outboxEventRepository.save(outboxEvent);

        return new RelayBoundaryScenario(order.getId(), payment.getId(), outboxEvent);
    }

    private OutboxEventEntity newReadyPaymentEvent(Instant now, int index) {
        OutboxEventEntity event = TestFixtures.pendingPaymentOutboxEvent(
                UUID.randomUUID(),
                OutboxEventType.PAYMENT_CONFIRMED
        );
        event.setCreatedAt(now.plusMillis(index));
        event.setUpdatedAt(now.plusMillis(index));
        event.setNextAttemptAt(now.minusSeconds(1));

        return event;
    }

    private OutboxEventEntity newReadyPaymentEvent(UUID paymentId, UUID orderId, Instant now) {
        OutboxEventEntity event = TestFixtures.pendingPaymentOutboxEvent(
                paymentId,
                OutboxEventType.PAYMENT_CONFIRMED
        );
        event.setMessageKey(orderId.toString());
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        event.setNextAttemptAt(now.minusSeconds(1));

        return event;
    }

    private void assertPublishedMessages(List<OutboxEventEntity> readyEvents) {
        List<OutboxMessage> messages = messagePublisher.messages();

        assertThat(messages).hasSize(readyEvents.size());
        assertThat(messages)
                .extracting(OutboxMessage::eventId)
                .containsExactlyInAnyOrderElementsOf(eventIds(readyEvents));
        assertThat(messages)
                .allSatisfy(message -> {
                    assertThat(message.topic()).isEqualTo("techora.payment.events");
                    assertThat(message.payload()).isNotBlank();
                    assertThat(message.headers()).isNotNull();
                });
    }

    private void assertRowsPublished(List<OutboxEventEntity> readyEvents) {
        List<UUID> rowIds = rowIds(readyEvents);
        List<OutboxEventEntity> publishedRows = outboxEventRepository.findAllById(rowIds);

        assertThat(publishedRows)
                .hasSize(readyEvents.size())
                .allSatisfy(event -> {
                    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
                    assertThat(event.getProcessedAt()).isNotNull();
                    assertThat(event.getLockedAt()).isNull();
                    assertThat(event.getLockedBy()).isNull();
                    assertThat(event.getLastError()).isNull();
                    assertThat(event.getRetryCount()).isZero();
                });
    }

    private void assertRowScheduledForRetry(OutboxEventEntity readyEvent, Instant beforeRelay) {
        OutboxEventEntity retryRow = outboxEventRepository.findById(readyEvent.getId()).orElseThrow();

        assertThat(retryRow.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
        assertThat(retryRow.getRetryCount()).isEqualTo(1);
        assertThat(retryRow.getLastError()).isEqualTo("Kafka unavailable");
        assertThat(retryRow.getNextAttemptAt()).isAfterOrEqualTo(beforeRelay);
        assertThat(retryRow.getProcessedAt()).isNull();
        assertThat(retryRow.getFailedAt()).isNull();
        assertThat(retryRow.getLockedAt()).isNull();
        assertThat(retryRow.getLockedBy()).isNull();
    }

    private void assertRowMarkedFailed(OutboxEventEntity exhaustedEvent) {
        OutboxEventEntity failedRow = outboxEventRepository.findById(exhaustedEvent.getId()).orElseThrow();

        assertThat(failedRow.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(failedRow.getRetryCount()).isEqualTo(retryProperties.maxRetries() + 1);
        assertThat(failedRow.getLastError()).isEqualTo("Kafka still unavailable");
        assertThat(failedRow.getFailedAt()).isNotNull();
        assertThat(failedRow.getProcessedAt()).isNull();
        assertThat(failedRow.getLockedAt()).isNull();
        assertThat(failedRow.getLockedBy()).isNull();
    }

    private void assertBusinessStateUnchanged(RelayBoundaryScenario scenario) {
        OrderJpaEntity order = orderRepository.findById(scenario.orderId()).orElseThrow();
        PaymentJpaEntity payment = paymentRepository.findById(scenario.paymentId()).orElseThrow();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(orderHistoryRepository.findByOrderIdOrderByCreatedAtAsc(scenario.orderId())).isEmpty();
    }

    private List<UUID> rowIds(List<OutboxEventEntity> events) {
        return events.stream()
                .map(OutboxEventEntity::getId)
                .toList();
    }

    private List<UUID> eventIds(List<OutboxEventEntity> events) {
        return events.stream()
                .map(OutboxEventEntity::getEventId)
                .toList();
    }

    private record RelayBoundaryScenario(
            UUID orderId,
            UUID paymentId,
            OutboxEventEntity outboxEvent
    ) {
    }

    @TestConfiguration
    static class OutboxRelayServiceTestConfig {

        @Bean
        @Primary
        CapturingOutboxMessagePublisher capturingOutboxMessagePublisher() {
            return new CapturingOutboxMessagePublisher();
        }
    }

    static class CapturingOutboxMessagePublisher implements OutboxMessagePublisher {

        private final List<OutboxMessage> messages = new CopyOnWriteArrayList<>();
        private RuntimeException failure;

        @Override
        public CompletableFuture<Void> publish(OutboxMessage message) {
            messages.add(message);
            if (failure != null) {
                return CompletableFuture.failedFuture(failure);
            }
            return CompletableFuture.completedFuture(null);
        }

        void failWith(RuntimeException failure) {
            this.failure = failure;
        }

        void clear() {
            messages.clear();
            failure = null;
        }

        List<OutboxMessage> messages() {
            return List.copyOf(messages);
        }
    }
}
