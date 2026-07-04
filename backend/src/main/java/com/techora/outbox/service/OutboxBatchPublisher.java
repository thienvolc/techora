package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxMessage;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.publisher.OutboxMessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class OutboxBatchPublisher {

    private final OutboxMessageFactory outboxMessageFactory;
    private final OutboxMessagePublisher messagePublisher;

    public List<PublishResult> publishBatch(List<OutboxEventEntity> events) {
        List<CompletableFuture<PublishResult>> publishedFutures = publishAll(events);
        return awaitAll(publishedFutures);
    }

    private List<CompletableFuture<PublishResult>> publishAll(List<OutboxEventEntity> events) {
        return events.stream()
                .map(this::publishSingleEvent)
                .toList();
    }

    private List<PublishResult> awaitAll(List<CompletableFuture<PublishResult>> futures) {
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private CompletableFuture<PublishResult> publishSingleEvent(OutboxEventEntity event) {
        try {
            OutboxMessage message = outboxMessageFactory.toMessage(event);
            return messagePublisher.publish(message)
                    .handle((ignoredResult, throwable) -> evaluateOutcome(event, throwable));
        } catch (RuntimeException ex) {
            return CompletableFuture.completedFuture(PublishResult.failure(event, ex));
        }
    }

    private PublishResult evaluateOutcome(OutboxEventEntity event, Throwable throwable) {
        if (throwable == null) {
            return PublishResult.success(event);
        }
        return PublishResult.failure(event, toRuntimeException(throwable));
    }

    private RuntimeException toRuntimeException(Throwable throwable) {
        Throwable cause = (throwable instanceof CompletionException ce && ce.getCause() != null)
                ? ce.getCause()
                : throwable;

        return cause instanceof RuntimeException rex
                ? rex
                : new IllegalStateException("Unexpected checked exception during publish", cause);
    }

    public record PublishResult(
            OutboxEventEntity event,
            RuntimeException failure
    ) {
        static PublishResult success(OutboxEventEntity event) {
            return new PublishResult(event, null);
        }

        static PublishResult failure(OutboxEventEntity event, RuntimeException failure) {
            return new PublishResult(event, failure);
        }

        public boolean isSuccess() {
            return failure == null;
        }
    }
}
