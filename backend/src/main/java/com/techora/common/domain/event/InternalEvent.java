package com.techora.common.domain.event;

import java.time.Instant;
import java.util.UUID;

public interface InternalEvent {
    UUID aggregateId();
    Instant occurredAt();
}