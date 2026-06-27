package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.command.CreatePaymentCommand;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.port.order.OrderPaymentPort;
import com.techora.payment.application.port.order.PreparedOrderForPayment;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.exception.PaymentAlreadyExistsException;
import com.techora.payment.domain.service.PaymentReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentCreator {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderPaymentPort orderPaymentPort;
    private final PaymentReferenceGenerator paymentReferenceGenerator;
    private final Clock clock;

    @Transactional
    public PaymentResult create(CreatePaymentCommand command) {
        PreparedOrderForPayment preparedOrder = prepareOrderForCreation(command);
        Payment pendingPayment = buildPendingPayment(preparedOrder, command);
        Payment savedPayment = saveOrThrowIfNotIdempotent(pendingPayment);
        return paymentMapper.toResult(savedPayment);
    }

    private Payment buildPendingPayment(PreparedOrderForPayment preparedOrder, CreatePaymentCommand command) {
        return Payment.createPending(
                preparedOrder.orderId(),
                preparedOrder.userId(),
                preparedOrder.username(),
                preparedOrder.total(),
                paymentReferenceGenerator.generate(),
                Instant.now(clock));
    }

    private PreparedOrderForPayment prepareOrderForCreation(CreatePaymentCommand command) {
        return orderPaymentPort.preparePayment(command.userId(), command.orderId());
    }

    private Payment saveOrThrowIfNotIdempotent(Payment payment) {
        try {
            return paymentRepository.save(payment);
        } catch (PaymentAlreadyExistsException ex) {
            throw new BusinessException(ResponseCode.PAYMENT_ALREADY_EXISTS);
        }
    }
}
