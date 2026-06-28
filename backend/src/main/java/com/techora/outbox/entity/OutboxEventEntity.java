package com.techora.outbox.entity;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_events_status_next_attempt", columnList = "status, next_attempt_at"),
        @Index(name = "idx_outbox_events_aggregate", columnList = "aggregate_type, aggregate_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false, length = 40)
    private OutboxAggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private OutboxEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    public void markProcessing(String instanceId, Instant now) {
        status = OutboxEventStatus.PROCESSING;
        lockedAt = now;
        lockedBy = instanceId;
        updatedAt = now;
    }

    public void markPublished(Instant now) {
        status = OutboxEventStatus.PUBLISHED;
        processedAt = now;
        updatedAt = now;
        lastError = null;
        lockedAt = null;
        lockedBy = null;
    }

    public void scheduleRetry(String errorMessage, Instant nextAttemptAt, Instant now) {
        status = OutboxEventStatus.PENDING;
        retryCount++;
        this.nextAttemptAt = nextAttemptAt;
        updatedAt = now;
        lastError = errorMessage;
        lockedAt = null;
        lockedBy = null;
    }

    public void markFailed(String errorMessage, Instant now) {
        status = OutboxEventStatus.FAILED;
        retryCount++;
        failedAt = now;
        updatedAt = now;
        lastError = errorMessage;
        lockedAt = null;
        lockedBy = null;
    }

    public void retryNow(Instant now) {
        status = OutboxEventStatus.PENDING;
        nextAttemptAt = now;
        failedAt = null;
        updatedAt = now;
        lastError = null;
        lockedAt = null;
        lockedBy = null;
    }
}
