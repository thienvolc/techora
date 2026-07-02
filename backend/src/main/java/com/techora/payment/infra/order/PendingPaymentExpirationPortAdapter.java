package com.techora.payment.infra.order;

import com.techora.order.application.port.payment.PendingPaymentExpirationPort;
import com.techora.order.application.port.payment.PendingPaymentExpirationResult;
import com.techora.payment.application.service.order.PendingPaymentExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PendingPaymentExpirationPortAdapter implements PendingPaymentExpirationPort {
    private final PendingPaymentExpirationService pendingPaymentExpirationService;

    @Override
    public PendingPaymentExpirationResult expirePendingPaymentForOrderTimeout(UUID orderId) {
        return switch (pendingPaymentExpirationService.expireForOrderTimeout(orderId)) {
            case EXPIRED -> PendingPaymentExpirationResult.EXPIRED;
            case ALREADY_PAID -> PendingPaymentExpirationResult.ALREADY_PAID;
            case NOT_PENDING -> PendingPaymentExpirationResult.NOT_PENDING;
            case NOT_FOUND -> PendingPaymentExpirationResult.NOT_FOUND;
        };
    }
}
