package com.techora.outbox.repository;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxEventBulkClaimer {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<OutboxEventEntity> claimReadyEventsByTypes(List<OutboxEventType> eventTypes,
                                                           String lockedBy,
                                                           int batchSize) {

        String sql = """
                update outbox_events
                set status = :processingStatus,
                    locked_at = :now,
                    locked_by = :lockedBy,
                    updated_at = :now
                where id in (
                    select id
                    from outbox_events
                    where status = :pendingStatus
                      and event_type in (:eventTypes)
                      and (next_attempt_at is null or next_attempt_at <= :now)
                    order by created_at asc
                    limit :batchSize
                    for update skip locked
                )
                returning *
                """;

        Instant now = Instant.now();
        List<String> eventTypeNames = eventTypes.stream()
                .map(OutboxEventType::name)
                .toList();
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("processingStatus", OutboxEventStatus.PROCESSING.name())
                .addValue("pendingStatus", OutboxEventStatus.PENDING.name())
                .addValue("eventTypes", eventTypeNames)
                .addValue("now", now)
                .addValue("lockedBy", lockedBy)
                .addValue("batchSize", batchSize);

        return namedParameterJdbcTemplate.query(
                sql,
                params,
                BeanPropertyRowMapper.newInstance(OutboxEventEntity.class)
        );
    }
}
