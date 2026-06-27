package com.techora.payment.application.service;

import com.techora.idempotency.command.IdempotencyCommand;
import com.techora.idempotency.command.IdempotencyParams;
import com.techora.idempotency.entity.IdempotencyOperation;
import com.techora.payment.application.command.InitiateVnPayPaymentCommand;
import com.techora.payment.application.result.InitiateVnPayPaymentResult;
import org.springframework.stereotype.Component;

@Component
public class PaymentIdempotencyFactory {

    public IdempotencyCommand<InitiateVnPayPaymentResult> createForVnPayInitiation(
            InitiateVnPayPaymentCommand command) {

        return IdempotencyCommand.<InitiateVnPayPaymentResult>builder()
                .userId(command.userId())
                .idempotencyKey(command.idempotencyKey())
                .operation(IdempotencyOperation.INITIATE_VNPAY_PAYMENT)
                .params(IdempotencyParams.paymentCreation(command.userId(), command.orderId()))
                .responseType(InitiateVnPayPaymentResult.class)
                .build();
    }
}
