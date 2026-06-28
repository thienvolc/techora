package com.techora.payment.infra.checkout;

import com.techora.checkout.application.port.payment.CheckoutPaymentCommand;
import com.techora.checkout.application.port.payment.CheckoutPaymentPort;
import com.techora.checkout.application.port.payment.CheckoutPaymentResult;
import com.techora.order.domain.entity.OrderStatus;
import com.techora.payment.application.command.CreatePaymentCommand;
import com.techora.payment.application.port.gateway.CreateVnPayPaymentRequest;
import com.techora.payment.application.port.gateway.VnPayGatewayPort;
import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.application.service.PaymentCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckoutPaymentPortAdapter implements CheckoutPaymentPort {
    private static final String ORDER_INFO_FORMAT = "Thanh toan don hang %s";

    private final PaymentCreator paymentCreator;
    private final VnPayGatewayPort vnPayGatewayPort;

    @Override
    public CheckoutPaymentResult initiate(CheckoutPaymentCommand command) {
        PaymentResult payment = paymentCreator.create(new CreatePaymentCommand(
                command.userId(),
                command.orderId(),
                command.paymentWindowExpiresAt(),
                null
        ));
        String paymentUrl = vnPayGatewayPort.buildPaymentUrl(CreateVnPayPaymentRequest.from(
                payment,
                command.ipAddress(),
                ORDER_INFO_FORMAT.formatted(payment.orderId())
        ));
        return new CheckoutPaymentResult(
                payment.id(),
                OrderStatus.PAYMENT_PENDING,
                paymentUrl,
                payment.expiresAt()
        );
    }
}
