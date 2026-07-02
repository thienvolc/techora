package com.techora.payment.infra.order;

import com.techora.order.application.service.payment.OrderPaymentService;
import com.techora.order.application.service.payment.PayableOrderSnapshot;
import com.techora.payment.application.port.order.OrderPaymentPort;
import com.techora.payment.application.port.order.PaymentConfirmationResult;
import com.techora.payment.application.port.order.PreparedOrderForPayment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentPortAdapter implements OrderPaymentPort {

    private final OrderPaymentService orderPaymentService;

    @Override
    public PreparedOrderForPayment preparePayment(UUID userId, UUID orderId) {
        PayableOrderSnapshot payableOrder = orderPaymentService.preparePayment(userId, orderId);
        return new PreparedOrderForPayment(
                payableOrder.orderId(),
                payableOrder.userId(),
                payableOrder.username(),
                payableOrder.total(),
                payableOrder.paymentDeadlineAt()
        );
    }

    @Override
    public PaymentConfirmationResult confirmPayment(UUID orderId,
                                                    String providerName) {

        return switch (orderPaymentService.confirmPayment(orderId, providerName)) {
            case CONFIRMED -> PaymentConfirmationResult.CONFIRMED;
            case ALREADY_PAID -> PaymentConfirmationResult.ALREADY_PAID;
            case NOT_PAYABLE -> PaymentConfirmationResult.NOT_PAYABLE;
        };
    }
}
