package com.techora.payment.application.usecase;

import com.techora.idempotency.IdempotencyCommandExecutor;
import com.techora.payment.application.command.CreatePaymentCommand;
import com.techora.payment.application.command.InitiateVnPayPaymentCommand;
import com.techora.payment.application.port.gateway.CreateVnPayPaymentRequest;
import com.techora.payment.application.port.gateway.VnPayGatewayPort;
import com.techora.payment.application.result.InitiateVnPayPaymentResult;
import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.application.service.PaymentCreator;
import com.techora.payment.application.service.PaymentIdempotencyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitiateVnPayPaymentUseCase {
    private static final String ORDER_INFO_FORMAT = "Thanh toan don hang %s";
    private final PaymentCreator paymentCreator;
    private final VnPayGatewayPort vnPayGatewayPort;
    private final IdempotencyCommandExecutor idempotencyCommandExecutor;
    private final PaymentIdempotencyFactory paymentIdempotencyFactory;

    @Transactional
    public InitiateVnPayPaymentResult execute(InitiateVnPayPaymentCommand command) {
        return idempotencyCommandExecutor.execute(
                paymentIdempotencyFactory.createForVnPayInitiation(command),
                () -> initiatePayment(command));
    }

    private InitiateVnPayPaymentResult initiatePayment(InitiateVnPayPaymentCommand command) {
        PaymentResult paymentResult = createPayment(command);
        String paymentUrl = buildPaymentUrl(paymentResult, command);
        return new InitiateVnPayPaymentResult(paymentResult.id(), paymentUrl);
    }

    private PaymentResult createPayment(InitiateVnPayPaymentCommand command) {
        return paymentCreator.create(
                new CreatePaymentCommand(
                        command.userId(),
                        command.orderId(),
                        command.idempotencyKey()
                ));
    }

    private String buildPaymentUrl(PaymentResult paymentResult, InitiateVnPayPaymentCommand command) {
        String orderInfo = ORDER_INFO_FORMAT.formatted(paymentResult.orderId());
        return vnPayGatewayPort.buildPaymentUrl(
                CreateVnPayPaymentRequest.from(paymentResult, command.ipAddress(), orderInfo)
        );
    }
}
