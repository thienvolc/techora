package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.payment.application.mapper.PaymentReconciliationMapper;
import com.techora.payment.application.model.PaymentReconciliationDetails;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.domain.entity.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentReconciliationMapper paymentReconciliationMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<PaymentReconciliationDetails> getUnresolved(Pageable pageable) {
        Page<PaymentAttempt> attempts = paymentAttemptRepository.findUnresolvedReconciliationRequired(pageable);
        return paymentReconciliationMapper.toPageResponse(attempts);
    }

    @Transactional(readOnly = true)
    public PaymentReconciliationDetails get(UUID attemptId) {
        return PaymentReconciliationDetails.from(getAttempt(attemptId));
    }

    @Transactional
    public PaymentReconciliationDetails resolve(UUID attemptId, String note) {
        PaymentAttempt attempt = resolveReconciliation(attemptId, note);
        return PaymentReconciliationDetails.from(attempt);
    }

    private PaymentAttempt resolveReconciliation(UUID attemptId, String note) {
        PaymentAttempt attempt = getAttempt(attemptId);
        attempt.resolveReconciliation(note, now());
        return paymentAttemptRepository.save(attempt);
    }

    private PaymentAttempt getAttempt(UUID attemptId) {
        return paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
