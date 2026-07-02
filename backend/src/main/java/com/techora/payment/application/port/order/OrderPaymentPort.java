package com.techora.payment.application.port.order;

import java.util.UUID;

public interface OrderPaymentPort {

    PreparedOrderForPayment preparePayment(UUID userId, UUID orderId);

    PaymentConfirmationResult confirmPayment(UUID orderId, String providerName);
}
