package com.techora.outbox.repository;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.dto.OutboxRelayOutcome;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventBulkUpdater {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<UUID> markPublished(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        if (outcomes.isEmpty()) {
            return List.of();
        }

        String sql = """
                update outbox_events
                set status = :publishedStatus,
                    processed_at = :now,
                    updated_at = :now,
                    last_error = null,
                    locked_at = null,
                    locked_by = null
                where id in (:ids)
                  and status = :processingStatus
                  and locked_by = :lockedBy
                returning id
                """;

        List<UUID> eventIds = outcomes.stream()
                .map(OutboxRelayOutcome::eventId)
                .toList();

        Instant now = outcomes.getFirst().occurredAt();

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("publishedStatus", OutboxEventStatus.PUBLISHED.name())
                .addValue("processingStatus", OutboxEventStatus.PROCESSING.name())
                .addValue("lockedBy", lockedBy)
                .addValue("now", Timestamp.from(now))
                .addValue("ids", eventIds);

        return namedParameterJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> rs.getObject("id", UUID.class)
        );
    }

    public int[] scheduleRetries(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        if (outcomes.isEmpty()) {
            return new int[0];
        }

        String sql = """
                update outbox_events
                set status = ?,
                    retry_count = retry_count + 1,
                    last_error = ?,
                    next_attempt_at = ?,
                    updated_at = ?,
                    locked_at = null,
                    locked_by = null
                where id = ?
                  and status = ?
                  and locked_by = ?
                """;

        return flatten(
                jdbcTemplate.batchUpdate(
                        sql,
                        outcomes,
                        outcomes.size(),
                        (statement, outcome) -> {
                            statement.setString(1, OutboxEventStatus.PENDING.name());
                            statement.setString(2, outcome.errorMessage());
                            statement.setTimestamp(3, Timestamp.from(outcome.nextAttemptAt()));
                            statement.setTimestamp(4, Timestamp.from(outcome.occurredAt()));
                            statement.setObject(5, outcome.eventId());
                            statement.setString(6, OutboxEventStatus.PROCESSING.name());
                            statement.setString(7, lockedBy);
                        }));
    }

    public int[] markFailed(List<OutboxRelayOutcome> outcomes, String lockedBy) {
        if (outcomes.isEmpty()) {
            return new int[0];
        }

        String sql = """
                update outbox_events
                set status = ?,
                    retry_count = retry_count + 1,
                    failed_at = ?,
                    updated_at = ?,
                    last_error = ?,
                    locked_at = null,
                    locked_by = null
                where id = ?
                  and status = ?
                  and locked_by = ?
                """;

        return flatten(
                jdbcTemplate.batchUpdate(
                        sql,
                        outcomes,
                        outcomes.size(),
                        (statement, outcome) -> {
                            statement.setString(1, OutboxEventStatus.FAILED.name());
                            statement.setTimestamp(2, Timestamp.from(outcome.occurredAt()));
                            statement.setTimestamp(3, Timestamp.from(outcome.occurredAt()));
                            statement.setString(4, outcome.errorMessage());
                            statement.setObject(5, outcome.eventId());
                            statement.setString(6, OutboxEventStatus.PROCESSING.name());
                            statement.setString(7, lockedBy);
                        }));
    }

    private int[] flatten(int[][] updateCounts) {
        return Arrays.stream(updateCounts)
                .flatMapToInt(Arrays::stream)
                .toArray();
    }
}
