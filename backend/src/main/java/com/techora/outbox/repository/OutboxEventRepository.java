package com.techora.outbox.repository;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);

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
