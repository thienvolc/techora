package com.techora.payment.application.usecase;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.mapper.PaymentMapper;
import com.techora.payment.application.model.VnPayReturnDetails;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.port.persistence.PaymentRepository;
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
    public VnPayReturnDetails execute(String txnRef) {
        PaymentAttempt attempt = getPaymentAttempt(txnRef);
        Payment payment = getPayment(attempt);

        return paymentMapper.toReturnDetails(payment, attempt);
    }

    private PaymentAttempt getPaymentAttempt(String txnRef) {
        return paymentAttemptRepository.findByProviderReference(txnRef)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private Payment getPayment(PaymentAttempt attempt) {
        return paymentRepository.findById(attempt.getPaymentId())
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }
}
