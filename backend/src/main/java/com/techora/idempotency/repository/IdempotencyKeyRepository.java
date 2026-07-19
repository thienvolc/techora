package com.techora.idempotency.repository;

import com.techora.idempotency.entity.IdempotencyKeyEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(:lockKey, 0))", nativeQuery = true)
    void acquireTransactionLock(@Param("lockKey") String lockKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdempotencyKeyEntity> findLockedByUserIdAndIdempotencyKey(UUID uuid, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<IdempotencyKeyEntity> findLockedById(UUID id);
}
