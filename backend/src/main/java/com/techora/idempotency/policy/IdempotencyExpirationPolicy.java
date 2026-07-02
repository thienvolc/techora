package com.techora.idempotency.policy;

import com.techora.idempotency.entity.IdempotencyOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class IdempotencyExpirationPolicy {
    private final Clock clock;

    public Instant expiresAt(IdempotencyOperation operation) {
        Instant now = Instant.now(clock);

        return switch (operation) {
            case PLACE_ORDER -> now.plus(30, ChronoUnit.MINUTES);
            case INITIATE_VNPAY_PAYMENT -> now.plus(1, ChronoUnit.DAYS);
        };
    }
}
