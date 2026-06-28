package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.result.PaymentReconciliationResult;
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
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<PaymentReconciliationResult> getUnresolved(Pageable pageable) {
        Page<PaymentAttempt> page = paymentAttemptRepository.findUnresolvedReconciliationRequired(pageable);
        return new PageResponse<>(
                page.getContent().stream()
                        .map(PaymentReconciliationResult::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PaymentReconciliationResult get(UUID attemptId) {
        return PaymentReconciliationResult.from(getAttempt(attemptId));
    }

    @Transactional
    public PaymentReconciliationResult resolve(UUID attemptId, String note) {
        PaymentAttempt attempt = getAttempt(attemptId);
        attempt.resolveReconciliation(note, Instant.now(clock));
        return PaymentReconciliationResult.from(paymentAttemptRepository.save(attempt));
    }

    private PaymentAttempt getAttempt(UUID attemptId) {
        return paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }
}
