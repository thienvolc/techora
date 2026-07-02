package com.techora.order.application.port.payment;

public interface PaymentInitiationPort {

    InitiatedOrderPayment initiate(InitiateOrderPaymentCommand command);
}
