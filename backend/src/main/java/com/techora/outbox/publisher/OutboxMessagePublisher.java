package com.techora.outbox.publisher;

import com.techora.outbox.dto.OutboxMessage;

import java.util.concurrent.CompletableFuture;

public interface OutboxMessagePublisher {

    CompletableFuture<Void> publish(OutboxMessage message);
}
