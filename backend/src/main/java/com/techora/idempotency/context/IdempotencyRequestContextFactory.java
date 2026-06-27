package com.techora.idempotency.context;

import com.techora.idempotency.command.IdempotencyCommand;
import com.techora.idempotency.service.IdempotencyRequestFingerprintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyRequestContextFactory {
    private final IdempotencyRequestFingerprintService fingerprintService;

    public <T> IdempotencyRequestContext<T> create(IdempotencyCommand<T> command) {
        return new IdempotencyRequestContext<>(
                command.userId(),
                command.idempotencyKey(),
                command.operation(),
                fingerprintService.fingerprint(
                        command.operation(),
                        command.requestAttributes()
                ),
                command.responseType()
        );
    }
}

