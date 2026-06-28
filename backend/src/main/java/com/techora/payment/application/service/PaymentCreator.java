package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.command.CreatePaymentCommand;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.policy.PaymentAttemptExpiryPolicy;
import com.techora.payment.application.port.order.OrderPaymentPort;
import com.techora.payment.application.port.order.PreparedOrderForPayment;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.exception.PaymentAlreadyExistsException;
import com.techora.payment.domain.service.PaymentReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentCreator {
    private static final PaymentProvider DEFAULT_PROVIDER = PaymentProvider.VNPAY;

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;
    private final OrderPaymentPort orderPaymentPort;
    private final PaymentReferenceGenerator paymentReferenceGenerator;
    private final PaymentAttemptExpiryPolicy paymentAttemptExpiryPolicy;
    private final Clock clock;

    @Transactional
    public PaymentResult create(CreatePaymentCommand command) {
        Optional<Payment> existingPayment = getExistingPayment(command);
        if (existingPayment.isPresent()) {
            return toResultWithReusableOrNewAttempt(existingPayment.get(), command);
        }

        PreparedOrderForPayment preparedOrder = prepareOrderForCreation(command);
        Payment pendingPayment = buildPendingPayment(preparedOrder, command);
        Payment savedPayment = saveOrGetReusablePendingPayment(pendingPayment);
        PaymentAttempt attempt = createAttempt(savedPayment);
        return paymentMapper.toResult(savedPayment, attempt);
    }

    private Optional<Payment> getExistingPayment(CreatePaymentCommand command) {
        return paymentRepository.findLockedByOrderIdAndUserId(command.orderId(), command.userId());
    }

    private PaymentResult toResultWithReusableOrNewAttempt(Payment payment, CreatePaymentCommand command) {
        Payment reusablePayment = reusablePendingPaymentOrThrow(payment);
        prepareOrderForCreation(command);
        PaymentAttempt attempt = paymentAttemptRepository.findReusablePendingByPaymentId(
                        reusablePayment.getId(),
                        Instant.now(clock)
                )
                .orElseGet(() -> createAttempt(reusablePayment));
        return paymentMapper.toResult(reusablePayment, attempt);
    }

    private Payment reusablePendingPaymentOrThrow(Payment payment) {
        if (payment.isPending()) {
            return payment;
        }
        throw new BusinessException(ResponseCode.PAYMENT_ALREADY_EXISTS);
    }

    private Payment buildPendingPayment(PreparedOrderForPayment preparedOrder, CreatePaymentCommand command) {
        Instant now = Instant.now(clock);
        return Payment.createPending(
                preparedOrder.orderId(),
                preparedOrder.userId(),
                preparedOrder.username(),
                preparedOrder.total(),
                paymentWindowExpiresAt(command, preparedOrder),
                now);
    }

    private Instant paymentWindowExpiresAt(CreatePaymentCommand command, PreparedOrderForPayment preparedOrder) {
        if (command.paymentWindowExpiresAt() != null) {
            return command.paymentWindowExpiresAt();
        }
        return preparedOrder.paymentWindowExpiresAt();
    }

    private PaymentAttempt createAttempt(Payment payment) {
        Instant now = Instant.now(clock);
        return paymentAttemptRepository.save(PaymentAttempt.createPending(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                DEFAULT_PROVIDER.name(),
                paymentReferenceGenerator.generate(),
                payment.getAmount(),
                paymentAttemptExpiryPolicy.attemptExpiresAt(now),
                now
        ));
    }

    private PreparedOrderForPayment prepareOrderForCreation(CreatePaymentCommand command) {
        return orderPaymentPort.preparePayment(command.userId(), command.orderId());
    }

    private Payment saveOrGetReusablePendingPayment(Payment payment) {
        try {
            return paymentRepository.save(payment);
        } catch (PaymentAlreadyExistsException ex) {
            return getExistingPayment(payment);
        }
    }

    private Payment getExistingPayment(Payment payment) {
        return paymentRepository.findLockedByOrderIdAndUserId(payment.getOrderId(), payment.getUserId())
                .map(this::reusablePendingPaymentOrThrow)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_ALREADY_EXISTS));
    }
}
