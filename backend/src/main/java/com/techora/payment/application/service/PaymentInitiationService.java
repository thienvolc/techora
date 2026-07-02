package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.command.CreatePaymentCommand;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.model.PaymentDetails;
import com.techora.payment.application.port.order.OrderPaymentPort;
import com.techora.payment.application.port.order.PreparedOrderForPayment;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentInitiationService {
    private final PaymentRepository paymentRepository;
    private final OrderPaymentPort orderPaymentPort;
    private final PaymentCreator paymentCreator;
    private final PaymentAttemptCreator paymentAttemptCreator;
    private final PaymentMapper paymentMapper;

    @Transactional
    public PaymentDetails initiate(CreatePaymentCommand command) {
        PreparedOrderForPayment preparedOrder = lockAndPrepareOrderForPayment(command);
        return getLockedExistingPayment(command)
                .map(payment -> reuseOrCreateAttempt(command, payment))
                .orElseGet(() -> createPaymentWithFirstAttempt(command, preparedOrder));
    }

    private PaymentDetails reuseOrCreateAttempt(CreatePaymentCommand command, Payment payment) {
        validateCanCreatePaymentAttempt(payment);
        PaymentAttempt attempt = paymentAttemptCreator.getOrCreatePendingAttempt(
                payment,
                command.provider()
        );
        return paymentMapper.toDetails(payment, attempt);
    }

    private PaymentDetails createPaymentWithFirstAttempt(CreatePaymentCommand command,
                                                         PreparedOrderForPayment preparedOrder) {

        Payment payment = paymentCreator.createPending(preparedOrder);
        PaymentAttempt attempt = paymentAttemptCreator.createPendingAttempt(
                payment,
                command.provider()
        );
        return paymentMapper.toDetails(payment, attempt);
    }

    private void validateCanCreatePaymentAttempt(Payment payment) {
        if (!payment.canCreateAttempt()) {
            throw new BusinessException(ResponseCode.PAYMENT_ALREADY_FINALIZED);
        }
    }

    private PreparedOrderForPayment lockAndPrepareOrderForPayment(CreatePaymentCommand command) {
        return orderPaymentPort.preparePayment(command.userId(), command.orderId());
    }

    private Optional<Payment> getLockedExistingPayment(CreatePaymentCommand command) {
        return paymentRepository.findLockedByOrderIdAndUserId(command.orderId(), command.userId());
    }
}
