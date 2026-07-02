package com.techora.payment.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.model.PaymentDetails;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public PaymentDetails execute(UUID paymentId, UUID userId) {
        Payment payment = getUserPayment(paymentId, userId);
        Optional<PaymentAttempt> latestAttempt = getLastestPaymentAttempt(payment);

        return latestAttempt.isEmpty()
                ? paymentMapper.toDetails(payment)
                : paymentMapper.toDetails(payment, latestAttempt.get());
    }

    private Payment getUserPayment(UUID paymentId, UUID userId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private Optional<PaymentAttempt> getLastestPaymentAttempt(Payment payment) {
        return paymentAttemptRepository.findLatestByPaymentId(payment.getId());
    }
}
