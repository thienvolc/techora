package com.techora.payment.application.port.order;

import java.util.UUID;

public interface OrderPaymentPort {

    PreparedOrderForPayment preparePayment(UUID userId, UUID orderId);

    void confirmPayment(UUID orderId, String providerName);

    void markPaymentFailedAndCancelOrder(UUID orderId, String providerName);
}
