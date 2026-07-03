package com.techora.outbox.scheduler;

import com.techora.common.infra.config.prop.OutboxRelayProperties;
import com.techora.outbox.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxRelayWorker {

    private final OutboxRelayService outboxRelayService;
    private final OutboxRelayProperties properties;

    @Scheduled(fixedDelayString = "${app.outbox.relay.delay-ms:${app.outbox.payment-worker.delay-ms:${app.outbox.worker-delay-ms:2000}}}")
    public int relayPendingEvents() {
        return outboxRelayService.relayPendingEvents(
                properties.eventTypes(),
                properties.batchSize());
    }
}
