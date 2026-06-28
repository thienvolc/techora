package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.command.ApplyProviderPaymentResultCommand;
import com.techora.payment.application.eventpublisher.PaymentEventPublisher;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
import com.techora.payment.domain.valueobject.ProviderPaymentEvidence;
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
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentEventPublisher paymentEventPublisher;
    private final Clock clock;

    @Transactional
    public void apply(ApplyProviderPaymentResultCommand command) {
        PaymentAttempt attempt = getAttempt(command.providerReference());
        Payment payment = getPayment(attempt.getPaymentId());
        validateAmountEquals(attempt, command.amount());
        if (command.successful()) {
            applySuccessfulProviderResult(payment, attempt, command);
            return;
        }

        applyFailedProviderResult(attempt, command);
    }

    private PaymentAttempt getAttempt(String providerReference) {
        return paymentAttemptRepository.findLockedByProviderReference(providerReference)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private Payment getPayment(java.util.UUID paymentId) {
        return paymentRepository.findLockedById(paymentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private void validateAmountEquals(PaymentAttempt attempt, BigDecimal amount) {
        if (attempt.getAmount().compareTo(amount) != 0) {
            throw new BusinessException(ResponseCode.PAYMENT_AMOUNT_MISMATCH);
        }
    }

    private void applySuccessfulProviderResult(Payment payment,
                                               PaymentAttempt attempt,
                                               ApplyProviderPaymentResultCommand command) {

        if (attempt.isProviderSuccessAlreadyHandled()) {
            return;
        }

        Instant now = Instant.now(clock);
        if (payment.isPending() && attempt.canAutoConfirm(now)) {
            attempt.markPaid(evidence(command, now), now);
            payment.markPaid(now);
            paymentAttemptRepository.save(attempt);
            paymentRepository.save(payment);
            paymentEventPublisher.publish(new PaymentConfirmedEvent(payment, providerName(command)));
            return;
        }

        PaymentReconciliationReason reason = reconciliationReason(payment, attempt, now);
        boolean attemptChanged = attempt.markReconciliationRequired(evidence(command, now), now);
        if (attemptChanged) {
            paymentAttemptRepository.save(attempt);
        }
        if (!payment.isProviderSuccessAlreadyHandled()) {
            payment.markReconciliationRequired(now);
            paymentRepository.save(payment);
        }
        if (attemptChanged) {
            paymentEventPublisher.publish(new PaymentReconciliationRequiredEvent(
                    payment,
                    attempt,
                    providerName(command),
                    reason
            ));
        }
    }

    private void applyFailedProviderResult(PaymentAttempt attempt, ApplyProviderPaymentResultCommand command) {
        if (!attempt.isPending()) {
            return;
        }

        Instant now = Instant.now(clock);
        boolean statusChanged = attempt.markFailed(evidence(command, now), now);
        if (statusChanged) {
            paymentAttemptRepository.save(attempt);
        }
    }

    private PaymentReconciliationReason reconciliationReason(Payment payment, PaymentAttempt attempt, Instant now) {
        if (attempt.isExpired(now)) {
            return PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED;
        }
        return payment.reconciliationReasonForSuccessfulProviderResult(now);
    }

    private ProviderPaymentEvidence evidence(ApplyProviderPaymentResultCommand command, Instant receivedAt) {
        return new ProviderPaymentEvidence(
                command.responseCode(),
                command.providerStatusCode(),
                command.providerTransactionId(),
                command.rawPayload(),
                receivedAt
        );
    }

    private String providerName(ApplyProviderPaymentResultCommand command) {
        return command.providerName().name();
    }
}
