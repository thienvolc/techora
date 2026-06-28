package com.techora.payment.infra.order;

import com.techora.order.application.payment.OrderPaymentService;
import com.techora.order.application.payment.PaymentPreparedOrder;
import com.techora.payment.application.port.order.OrderPaymentPort;
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
        PaymentPreparedOrder preparedOrder = orderPaymentService.preparePayment(userId, orderId);
        return new PreparedOrderForPayment(
                preparedOrder.orderId(),
                preparedOrder.userId(),
                preparedOrder.username(),
                preparedOrder.total(),
                preparedOrder.paymentWindowExpiresAt()
        );
    }

    @Override
    public com.techora.payment.application.port.order.OrderPaymentConfirmationResult confirmPayment(
            UUID orderId,
            String providerName) {

        return switch (orderPaymentService.confirmPayment(orderId, providerName)) {
            case CONFIRMED -> com.techora.payment.application.port.order.OrderPaymentConfirmationResult.CONFIRMED;
            case ALREADY_PAID -> com.techora.payment.application.port.order.OrderPaymentConfirmationResult.ALREADY_PAID;
            case NOT_PAYABLE -> com.techora.payment.application.port.order.OrderPaymentConfirmationResult.NOT_PAYABLE;
        };
    }
}
