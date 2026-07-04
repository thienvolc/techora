package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxRelayOutcome;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.policy.OutboxRetryPolicy;
import com.techora.outbox.service.OutboxBatchPublisher.PublishResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxStateResolver {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 4_000;

    private final OutboxRetryPolicy retryPolicy;

    public List<OutboxRelayOutcome> plan(List<PublishResult> results) {
        Instant now = Instant.now();

        return results.stream()
                .map(result -> plan(result, now))
                .toList();
    }

    public OutboxRelayOutcome plan(PublishResult result, Instant now) {
        OutboxEventEntity event = result.event();
        if (result.isSuccess()) {
            return buildPublished(event, now);
        }

        String errorMessage = getNormalizedErrorMessage(result.failure());
        if (canRetry(event)) {
            return buildRetry(event, errorMessage, now);
        }

        return buildFailed(event, errorMessage, now);
    }

    private OutboxRelayOutcome buildPublished(OutboxEventEntity event, Instant now) {
        return OutboxRelayOutcome.published(event.getId(), event.getEventType(), now);
    }

    private OutboxRelayOutcome buildRetry(OutboxEventEntity event, String errorMessage, Instant now) {
        return OutboxRelayOutcome.retryScheduled(
                event.getId(),
                event.getEventType(),
                errorMessage,
                retryPolicy.nextAttemptAt(event.nextRetryAttempt(), now),
                now
        );
    }

    private OutboxRelayOutcome buildFailed(OutboxEventEntity event, String errorMessage, Instant now) {
        return OutboxRelayOutcome.failed(
                event.getId(),
                event.getEventType(),
                errorMessage,
                now
        );
    }

    private boolean canRetry(OutboxEventEntity event) {
        return retryPolicy.canRetry(event.nextRetryAttempt());
    }

    private String getNormalizedErrorMessage(RuntimeException ex) {
        String message = getErrorMessage(ex);

        return needsTruncate(message)
                ? truncateMessage(message)
                : message;
    }

    private String getErrorMessage(RuntimeException ex) {
        return ex.getMessage() == null
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }

    private boolean needsTruncate(String message) {
        return message.length() > MAX_ERROR_MESSAGE_LENGTH;
    }

    private String truncateMessage(String message) {
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH);
    }
}
