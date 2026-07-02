package com.techora.payment.application.usecase;

import com.techora.idempotency.IdempotencyCommandExecutor;
import com.techora.payment.application.command.CreatePaymentCommand;
import com.techora.payment.application.command.InitiateVnPayPaymentCommand;
import com.techora.payment.domain.valueobject.PaymentProvider;
import com.techora.payment.application.model.PaymentDetails;
import com.techora.payment.application.model.VnPayPaymentSession;
import com.techora.payment.application.port.gateway.CreateVnPayPaymentRequest;
import com.techora.payment.application.port.gateway.VnPayGatewayPort;
import com.techora.payment.application.service.PaymentIdempotencyFactory;
import com.techora.payment.application.service.PaymentInitiationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InitiateVnPayPaymentUseCase {
    private static final String ORDER_INFO_FORMAT = "Thanh toan don hang %s";

    private final PaymentInitiationService paymentInitiationService;
    private final VnPayGatewayPort vnPayGatewayPort;
    private final IdempotencyCommandExecutor idempotencyCommandExecutor;
    private final PaymentIdempotencyFactory paymentIdempotencyFactory;

    @Transactional
    public VnPayPaymentSession execute(InitiateVnPayPaymentCommand command) {
        return idempotencyCommandExecutor.execute(
                paymentIdempotencyFactory.createForVnPayInitiation(command),
                () -> initiatePayment(command));
    }

    private VnPayPaymentSession initiatePayment(InitiateVnPayPaymentCommand command) {
        PaymentDetails payment = createPayment(command);
        String paymentUrl = buildPaymentUrl(payment, command);

        return new VnPayPaymentSession(
                payment.id(),
                paymentUrl,
                payment.expiresAt());
    }

    private PaymentDetails createPayment(InitiateVnPayPaymentCommand command) {
        return paymentInitiationService.initiate(
                new CreatePaymentCommand(
                        command.userId(),
                        command.orderId(),
                        PaymentProvider.VNPAY
                ));
    }

    private String buildPaymentUrl(PaymentDetails payment, InitiateVnPayPaymentCommand command) {
        String orderInfo = ORDER_INFO_FORMAT.formatted(payment.orderId());
        return vnPayGatewayPort.buildPaymentUrl(
                CreateVnPayPaymentRequest.from(payment, command.ipAddress(), orderInfo));
    }
}
