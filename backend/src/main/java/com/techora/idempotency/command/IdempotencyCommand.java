package com.techora.idempotency.command;

import com.techora.common.application.util.StringUtils;
import com.techora.idempotency.entity.IdempotencyOperation;
import jakarta.annotation.Nullable;
import lombok.Builder;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Builder
public record IdempotencyCommand<T>(
        UUID userId,
        @Nullable String idempotencyKey,
        IdempotencyOperation operation,
        IdempotencyParams params,
        Class<T> responseType) {

    public IdempotencyCommand {
        Objects.requireNonNull(userId, "User id is required");
        Objects.requireNonNull(operation, "Idempotency operation is required");
        Objects.requireNonNull(params, "Idempotency params are required");
        Objects.requireNonNull(responseType, "Idempotency response type is required");

        idempotencyKey = StringUtils.trimToNull(idempotencyKey);
    }

    public Map<String, Object> requestAttributes() {
        return params.attributes();
    }

    public boolean hasKey() {
        return idempotencyKey != null;
    }
}
