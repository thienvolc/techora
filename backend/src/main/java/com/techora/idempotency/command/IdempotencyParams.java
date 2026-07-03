package com.techora.idempotency.command;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record IdempotencyParams(
        Map<String, Object> attributes) {

    private static final String USER_ID = "userId";
    private static final String ORDER_ID = "orderId";

    public IdempotencyParams {
        attributes = Map.copyOf(
                Objects.requireNonNull(
                        attributes,
                        "Idempotency data are required"));
    }

    public static IdempotencyParams placeOrder(UUID userId) {
        return new IdempotencyParams(
                Map.of(
                        USER_ID, userId));
    }

    public static IdempotencyParams paymentCreation(UUID userId, UUID orderId) {
        return new IdempotencyParams(
                Map.of(
                        USER_ID, userId,
                        ORDER_ID, orderId));
    }
}
