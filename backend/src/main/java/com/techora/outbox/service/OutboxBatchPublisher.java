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
        List<CompletableFuture<PublishResult>> futures = events.stream()
                .map(this::publishAsync)
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private CompletableFuture<PublishResult> publishAsync(OutboxEventEntity event) {
        try {
            OutboxMessage message = outboxMessageFactory.toMessage(event);
            return messagePublisher.publish(message)
                    .handle((ignored, throwable) ->
                            throwable == null
                                    ? PublishResult.success(event)
                                    : PublishResult.failure(event, toRuntimeException(throwable)));
        } catch (RuntimeException ex) {
            return CompletableFuture.completedFuture(PublishResult.failure(event, ex));
        }
    }

    private RuntimeException toRuntimeException(Throwable throwable) {
        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(cause);
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

        public boolean succeeded() {
            return failure == null;
        }
    }
}
