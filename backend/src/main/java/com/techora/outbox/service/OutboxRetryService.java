package com.techora.outbox.service;

import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxRetryService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public boolean retryFailedEvent(UUID eventId) {
        return outboxEventRepository.findById(eventId)
                .filter(OutboxEventEntity::isFailed)
                .map(this::retryEvent)
                .orElse(false);
    }

    private boolean retryEvent(OutboxEventEntity event) {
        event.retryNow(Instant.now());
        return true;
    }
}
