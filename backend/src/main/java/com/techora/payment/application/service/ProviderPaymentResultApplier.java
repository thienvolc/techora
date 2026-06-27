package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.command.ApplyProviderPaymentResultCommand;
import com.techora.payment.application.eventpublisher.PaymentEventPublisher;
import com.techora.payment.application.port.order.OrderPaymentPort;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProviderPaymentResultApplier {
    private final PaymentRepository paymentRepository;
    private final OrderPaymentPort orderPaymentPort;
    private final PaymentEventPublisher paymentEventPublisher;
    private final Clock clock;

    @Transactional
    public void apply(ApplyProviderPaymentResultCommand command) {
        Payment payment = getPayment(command.providerReference());
        validateAmountEquals(payment, command.amount());
        boolean statusChangedSuccess = applyProviderStatus(payment, command.successful());
        if (statusChangedSuccess) {
            applyProviderStatusForOrder(payment, command);
            publishEvent(payment, command.successful());
        }
    }

    private Payment getPayment(String providerReference) {
        return paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private void validateAmountEquals(Payment payment, BigDecimal amount) {
        if (payment.getAmount().compareTo(amount) != 0) {
            throw new BusinessException(ResponseCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private boolean applyProviderStatus(Payment payment, boolean successful) {
        boolean statusChangedSuccess = successful
                ? payment.markPaid(Instant.now(clock))
                : payment.markFailed(Instant.now(clock));
        if (statusChangedSuccess) {
            paymentRepository.save(payment);
        }
        return statusChangedSuccess;
    }

    private void applyProviderStatusForOrder(Payment payment,
                                             ApplyProviderPaymentResultCommand command) {

        String providerName = command.providerName().name();
        if (command.successful()) {
            orderPaymentPort.confirmPayment(payment.getOrderId(), providerName);
        } else {
            orderPaymentPort.markPaymentFailedAndCancelOrder(payment.getOrderId(), providerName);
        }
    }

    private void publishEvent(Payment payment, boolean successful) {
        if (successful) {
            paymentEventPublisher.publish(new PaymentConfirmedEvent(payment));
        } else {
            paymentEventPublisher.publish(new PaymentFailedEvent(payment));
        }
    }
}
