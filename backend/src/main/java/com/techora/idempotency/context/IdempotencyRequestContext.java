package com.techora.idempotency.context;

import com.techora.idempotency.entity.IdempotencyOperation;

import java.util.UUID;

public record IdempotencyRequestContext<T>(
        UUID userId,
        String idempotencyKey,
        IdempotencyOperation operation,
        String requestHash,
        Class<T> responseType
) {
    public boolean hasKey() {
        return idempotencyKey != null && !idempotencyKey.isBlank();
    }
}
