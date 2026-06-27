package com.techora.idempotency;

import com.techora.idempotency.command.IdempotencyCommand;
import com.techora.idempotency.entity.IdempotencyKeyEntity;
import com.techora.idempotency.service.IdempotencyKeyStore;
import com.techora.idempotency.service.IdempotentCommandHandler;
import com.techora.idempotency.context.IdempotencyRequestContext;
import com.techora.idempotency.context.IdempotencyRequestContextFactory;
import com.techora.idempotency.service.IdempotencyResponseCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdempotencyCommandExecutor {
    private final IdempotencyRequestContextFactory contextFactory;
    private final IdempotencyKeyStore keyStore;
    private final IdempotencyResponseCodec responseCodec;

    public <T> T execute(IdempotencyCommand<T> command,
                         IdempotentCommandHandler<T> handler) {

        IdempotencyRequestContext<T> context = contextFactory.create(command);

        if (!context.hasKey()) {
            return handler.handle();
        }

        IdempotencyKeyEntity key = keyStore.acquire(context);

        if (key.isCompleted()) {
            return responseCodec.deserialize(key.getResponsePayload(), context.responseType());
        }

        return executeAndComplete(key, handler);
    }

    private <T> T executeAndComplete(IdempotencyKeyEntity key,
                                     IdempotentCommandHandler<T> handler) {
        try {
            T response = handler.handle();
            keyStore.complete(key, response);
            return response;
        } catch (RuntimeException ex) {
            keyStore.fail(key, ex);
            throw ex;
        }
    }
}
