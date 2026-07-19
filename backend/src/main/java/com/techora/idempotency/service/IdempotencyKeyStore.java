package com.techora.idempotency.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.idempotency.entity.IdempotencyKeyEntity;
import com.techora.idempotency.policy.IdempotencyExpirationPolicy;
import com.techora.idempotency.repository.IdempotencyKeyRepository;
import com.techora.idempotency.context.IdempotencyRequestContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyKeyStore {
    private final IdempotencyKeyRepository keyRepository;
    private final IdempotencyExpirationPolicy expirationPolicy;
    private final IdempotencyResponseCodec responseCodec;
    private final Clock clock;

    @Transactional
    public IdempotencyKeyEntity acquire(IdempotencyRequestContext<?> context) {
        acquireTransactionLock(context);
        return keyRepository
                .findLockedByUserIdAndIdempotencyKey(
                        context.userId(),
                        context.idempotencyKey()
                )
                .map(existing -> resolveExisting(existing, context))
                .orElseGet(() -> createProcessingKey(context));
    }

    private void acquireTransactionLock(IdempotencyRequestContext<?> context) {
        keyRepository.acquireTransactionLock(lockKey(context));
    }

    private String lockKey(IdempotencyRequestContext<?> context) {
        return context.userId() + ":" + context.idempotencyKey();
    }

    @Transactional
    public void complete(IdempotencyKeyEntity key, Object response) {
        IdempotencyKeyEntity managedKey = getForUpdateOrThrow(key.getId());
        managedKey.markCompleted(responseCodec.serialize(response), now());
    }

    private IdempotencyKeyEntity getForUpdateOrThrow(UUID keyId) {
        return keyRepository.findLockedById(keyId).orElseThrow();
    }

    private IdempotencyKeyEntity resolveExisting(IdempotencyKeyEntity existingKey,
                                                 IdempotencyRequestContext<?> context) {
        if (existingKey.isExpired(now())) {
            throw new BusinessException(ResponseCode.IDEMPOTENCY_KEY_EXPIRED);
        }

        if (!existingKey.matches(context.operation(), context.requestHash())) {
            throw new BusinessException(ResponseCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        if (existingKey.isCompleted()) {
            return existingKey;
        }

        if (existingKey.isProcessing()) {
            throw new BusinessException(ResponseCode.IDEMPOTENCY_REQUEST_PROCESSING);
        }

        throw new BusinessException(ResponseCode.IDEMPOTENCY_REQUEST_PROCESSING);
    }

    private IdempotencyKeyEntity createProcessingKey(IdempotencyRequestContext<?> context) {
        IdempotencyKeyEntity key = IdempotencyKeyEntity.processing(
                context.userId(),
                context.idempotencyKey(),
                context.operation(),
                context.requestHash(),
                now(),
                expirationPolicy.expiresAt(context.operation())
        );
        return keyRepository.save(key);
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
