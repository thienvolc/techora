package com.techora.outbox.port;

import com.techora.outbox.dto.OutboxMessage;

public interface OutboxMessagePublisher {

    void publish(OutboxMessage message);
}
