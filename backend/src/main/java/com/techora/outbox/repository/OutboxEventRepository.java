package com.techora.outbox.repository;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    @Query(value = """
            select *
            from outbox_events
            where status = :status
              and (next_attempt_at is null or next_attempt_at <= :now)
            order by created_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> findReadyEvents(
            @Param("status") String status,
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Query(value = """
            select *
            from outbox_events
            where status = :status
              and event_type = :eventType
              and (next_attempt_at is null or next_attempt_at <= :now)
            order by created_at asc
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> findReadyEventsByType(
            @Param("status") String status,
            @Param("eventType") String eventType,
            @Param("now") Instant now,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = """
            update outbox_events
            set status = :pendingStatus,
                next_attempt_at = :now,
                updated_at = :now,
                locked_at = null,
                locked_by = null
            where status = :processingStatus
              and locked_at <= :staleBefore
            """, nativeQuery = true)
    int releaseStaleProcessingEvents(
            @Param("processingStatus") String processingStatus,
            @Param("pendingStatus") String pendingStatus,
            @Param("staleBefore") Instant staleBefore,
            @Param("now") Instant now
    );

    List<OutboxEventEntity> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            OutboxAggregateType aggregateType,
            UUID aggregateId
    );

    long countByAggregateTypeAndAggregateIdAndEventType(
            OutboxAggregateType aggregateType,
            UUID aggregateId,
            OutboxEventType eventType
    );
}
