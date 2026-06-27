package com.techora.payment.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.domain.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public PaymentResult execute(UUID paymentId, UUID userId) {
        Payment payment = getPayment(paymentId, userId);
        return paymentMapper.toResult(payment);
    }

    private Payment getPayment(UUID paymentId, UUID userId) {
        return paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }
}
