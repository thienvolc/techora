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
        PaymentPreparedOrder preparedOrder = orderPaymentService.preparePayment(orderId);
        return new PreparedOrderForPayment(
                preparedOrder.orderId(),
                preparedOrder.userId(),
                preparedOrder.username(),
                preparedOrder.total()
        );
    }

    @Override
    public void confirmPayment(UUID orderId, String providerName) {
        orderPaymentService.confirmPayment(orderId, providerName);
    }

    @Override
    public void markPaymentFailedAndCancelOrder(UUID orderId, String providerName) {
        orderPaymentService.markPaymentFailedAndCancelOrder(orderId, providerName);
    }
}
