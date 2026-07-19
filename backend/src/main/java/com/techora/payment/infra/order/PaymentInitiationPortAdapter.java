package com.techora.payment.infra.order;

import com.techora.order.application.port.payment.InitiateOrderPaymentCommand;
import com.techora.order.application.port.payment.InitiatedOrderPayment;
import com.techora.order.application.port.payment.PaymentInitiationPort;
import com.techora.payment.application.command.InitiateVnPayPaymentCommand;
import com.techora.payment.application.model.VnPayPaymentSession;
import com.techora.payment.application.usecase.InitiateVnPayPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentInitiationPortAdapter implements PaymentInitiationPort {

    private final InitiateVnPayPaymentUseCase initiateVnPayPaymentUseCase;

    @Override
    public InitiatedOrderPayment initiate(InitiateOrderPaymentCommand command) {
        VnPayPaymentSession paymentSession =
                initiateVnPayPaymentUseCase.execute(new InitiateVnPayPaymentCommand(
                        command.userId(),
                        command.orderId(),
                        command.ipAddress(),
                        paymentInitiationKey(command.orderId())
                ));
        return new InitiatedOrderPayment(
                paymentSession.paymentId(),
                paymentSession.paymentUrl(),
                paymentSession.expiresAt()
        );
    }

    private String paymentInitiationKey(UUID orderId) {
        return "order-payment-" + orderId;
    }
}
