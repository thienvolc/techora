package com.techora.payment.infra.order;

import com.techora.payment.application.port.order.OrderPaymentPort;
import com.techora.payment.application.port.order.PaymentConfirmationResult;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentOrderEventListener {

    private final OrderPaymentPort orderPaymentPort;

    @EventListener
    public void on(PaymentConfirmedEvent event) {
        PaymentConfirmationResult result = orderPaymentPort.confirmPayment(
                event.orderId(),
                event.providerName().name()
        );
        if (result == PaymentConfirmationResult.NOT_PAYABLE) {
            log.warn("Payment confirmed but order is not payable. orderId={}, provider={}",
                    event.orderId(),
                    event.providerName());
        }
    }
}
