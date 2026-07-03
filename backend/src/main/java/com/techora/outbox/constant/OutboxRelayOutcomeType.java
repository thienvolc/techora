package com.techora.outbox.constant;

public enum OutboxRelayOutcomeType {
    PUBLISHED,
    RETRY_SCHEDULED,
    FAILED
}
