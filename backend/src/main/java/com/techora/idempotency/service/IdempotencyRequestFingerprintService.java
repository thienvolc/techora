package com.techora.idempotency.service;

import com.techora.idempotency.entity.IdempotencyOperation;
import com.techora.common.infra.service.CryptoService;
import com.techora.common.infra.service.JsonCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class IdempotencyRequestFingerprintService {

    private static final String OPERATION = "operation";

    private final JsonCodec jsonCodec;
    private final CryptoService cryptoService;

    public String fingerprint(IdempotencyOperation operation,
                              Map<String, Object> attributes) {

        var canonicalAttributes = toCanonicalAttributes(operation, attributes);
        return cryptoService.sha256(
                jsonCodec.toJson(canonicalAttributes));
    }

    private Map<String, Object> toCanonicalAttributes(IdempotencyOperation operation,
                                                      Map<String, Object> attributes) {

        Map<String, Object> canonicalAttributes = new TreeMap<>(attributes);
        canonicalAttributes.put(OPERATION, operation.name());
        return canonicalAttributes;
    }
}
