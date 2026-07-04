package com.techora.outbox.service;

import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventBulkClaimer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventClaimer {

    private final OutboxEventBulkClaimer bulkClaimer;

    @Transactional
    public List<OutboxEventEntity> claim(@NonNull List<OutboxEventType> eventTypes,
                                         int batchSize,
                                         String claimedBy) {

        if (eventTypes.isEmpty()) {
            return List.of();
        }

        return bulkClaimer.claimReadyEventsByTypes(eventTypes, claimedBy, batchSize);
    }
}
