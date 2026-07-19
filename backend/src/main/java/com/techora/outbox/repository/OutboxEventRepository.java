package com.techora.outbox.repository;

import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.constant.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Modifying
    @Query("""
            update OutboxEventEntity e
            set e.status = :pendingStatus,
                e.nextAttemptAt = :now,
                e.updatedAt = :now,
                e.lockedAt = null,
                e.lockedBy = null
            where e.status = :processingStatus
              and e.lockedAt <= :staleBefore
            """)
    int releaseStaleProcessingEvents(
            @Param("processingStatus") OutboxEventStatus processingStatus,
            @Param("pendingStatus") OutboxEventStatus pendingStatus,
            @Param("staleBefore") Instant staleBefore,
            @Param("now") Instant now
    );
}
