package com.techora.payment.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.application.result.PaymentResult;
import com.techora.payment.application.result.VnPayReturnResult;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetVnPayReturnUseCase {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public VnPayReturnResult execute(String txnRef) {
        PaymentAttempt attempt = getAttempt(txnRef);
        Payment payment = getPayment(attempt);
        PaymentResult result = paymentMapper.toResult(payment, attempt);
        return VnPayReturnResult.from(result);
    }

    private PaymentAttempt getAttempt(String txnRef) {
        return paymentAttemptRepository.findByProviderReference(txnRef)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private Payment getPayment(PaymentAttempt attempt) {
        return paymentRepository.findById(attempt.getPaymentId())
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }
}
