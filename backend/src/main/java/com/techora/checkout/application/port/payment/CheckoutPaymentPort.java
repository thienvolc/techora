package com.techora.checkout.application.port.payment;

public interface CheckoutPaymentPort {

    CheckoutPaymentResult initiate(CheckoutPaymentCommand command);
}
