package com.techora.payment.infra.outbox.schema;

import java.time.Instant;

public interface PaymentEventPayload {

    Instant occurredAt();
}
