package com.techora.payment.application.port.order;

import java.util.UUID;

public interface OrderPaymentPort {

    PreparedOrderForPayment preparePayment(UUID userId, UUID orderId);

    OrderPaymentConfirmationResult confirmPayment(UUID orderId, String providerName);
}
