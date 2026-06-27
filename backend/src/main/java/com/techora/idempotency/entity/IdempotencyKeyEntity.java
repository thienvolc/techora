package com.techora.idempotency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys", uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_keys_user_key", columnNames = {"user_id", "idempotency_key"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IdempotencyOperation operation;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_payload", nullable = false, columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markCompleted(IdempotencyOperation operation,
                              String requestHash,
                              String responsePayload,
                              Instant updatedAt,
                              Instant expiresAt) {

        this.operation = operation;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.COMPLETED;
        this.responsePayload = responsePayload;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }

    public boolean isProcessing() {
        return status == IdempotencyStatus.PROCESSING;
    }

    public boolean matches(IdempotencyOperation operation, String requestHash) {
        return this.operation == operation && this.requestHash.equals(requestHash);
    }

    public void markProcessing(IdempotencyOperation operation,
                               String idempotencyKey,
                               Instant now,
                               Instant expiresAt) {

        this.operation = operation;
        this.idempotencyKey = idempotencyKey;
        this.updatedAt = now;
        this.status = IdempotencyStatus.PROCESSING;
        this.expiresAt = expiresAt;
    }


    public static IdempotencyKeyEntity processing(UUID userId,
                                                  String idempotencyKey,
                                                  IdempotencyOperation operation,
                                                  String requestHash,
                                                  Instant now,
                                                  Instant expiresAt) {

        return IdempotencyKeyEntity.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .operation(operation)
                .requestHash(requestHash)
                .status(IdempotencyStatus.PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    public void markCompleted(String responsePayload, Instant updatedAt) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responsePayload = responsePayload;
        this.updatedAt = updatedAt;
    }

    public void markFailed(String responsePayload, Instant updatedAt) {
        this.status = IdempotencyStatus.FAILED;
        this.responsePayload = responsePayload;
        this.updatedAt = updatedAt;
    }
}
