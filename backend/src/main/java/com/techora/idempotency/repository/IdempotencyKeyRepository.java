package com.techora.idempotency.repository;

import com.techora.idempotency.entity.IdempotencyKeyEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<IdempotencyKeyEntity> findLockedByUserIdAndIdempotencyKey(UUID uuid, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<IdempotencyKeyEntity> findLockedById(UUID id);
}
