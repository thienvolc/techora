package com.techora.payment.infra.order;

import com.techora.order.application.port.payment.PaymentWindowExpirationPort;
import com.techora.order.application.port.payment.PaymentWindowExpirationResult;
import com.techora.payment.application.service.PaymentWindowExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentWindowExpirationPortAdapter implements PaymentWindowExpirationPort {
    private final PaymentWindowExpirationService paymentWindowExpirationService;

    @Override
    public PaymentWindowExpirationResult expirePaymentWindow(UUID orderId) {
        return switch (paymentWindowExpirationService.expireByOrderId(orderId)) {
            case EXPIRED -> PaymentWindowExpirationResult.EXPIRED;
            case ALREADY_PAID -> PaymentWindowExpirationResult.ALREADY_PAID;
            case NOT_EXPIRABLE -> PaymentWindowExpirationResult.NOT_EXPIRABLE;
            case NOT_FOUND -> PaymentWindowExpirationResult.NOT_FOUND;
        };
    }
}
