package com.techora.payment.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.payment.application.command.ProcessPaymentResultCommand;
import com.techora.payment.application.eventpublisher.PaymentEventPublisher;
import com.techora.payment.application.model.WebhookProcessResult;
import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.application.port.persistence.PaymentRepository;
import com.techora.payment.domain.entity.Payment;
import com.techora.payment.domain.entity.PaymentAttempt;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import com.techora.payment.domain.event.PaymentReconciliationRequiredEvent;
import com.techora.payment.domain.valueobject.PaymentReconciliationReason;
import com.techora.payment.domain.valueobject.ProviderPaymentEvidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookProcessor {
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public WebhookProcessResult process(ProcessPaymentResultCommand command) {
        // INFO: to prevent deadlock we need to lock the parent to child (payment -> attempt)
        PaymentAttempt unlockedAttempt = getUnlockedAttempt(command.providerReference());
        Payment payment = getLockedPayment(unlockedAttempt);
        PaymentAttempt attempt = getLockedAttempt(unlockedAttempt);

        if (attempt.hasHandledProviderResult()) {
            log.info("Webhook already handled for attempt {}", attempt.getId());
            return WebhookProcessResult.ALREADY_HANDLED;
        }

        return applyResult(payment, attempt, command);
    }

    private WebhookProcessResult applyResult(Payment payment, PaymentAttempt attempt, ProcessPaymentResultCommand command) {
        ProviderPaymentEvidence evidence = buildEvidence(command);

        if (!command.successful()) {
            applyFailure(attempt, evidence);
            return WebhookProcessResult.SUCCESS;
        } else if (isAmountMismatched(attempt, command.amount())) {
            applyAmountMismatch(payment, attempt, evidence);
            return WebhookProcessResult.AMOUNT_MISMATCH;
        } else {
            applySuccess(payment, attempt, evidence);
            return WebhookProcessResult.SUCCESS;
        }
    }

    private void applyAmountMismatch(Payment payment, PaymentAttempt attempt, ProviderPaymentEvidence evidence) {
        log.info("Amount mismatched for attempt {}. Expect: {}, Actual: {}",
                attempt.getId(), attempt.getAmount(), evidence.amount());

        PaymentReconciliationReason reason = PaymentReconciliationReason.AMOUNT_MISMATCHED;

        attempt.markReconciliationRequired(evidence, reason);
        paymentAttemptRepository.save(attempt);

        payment.markReconciliationRequired(evidence.receivedAt());
        paymentRepository.save(payment);

        paymentEventPublisher.publish(new PaymentReconciliationRequiredEvent(attempt, reason));
    }

    private void applyFailure(PaymentAttempt attempt, ProviderPaymentEvidence evidence) {
        if (!attempt.canApplyProviderFailure()) {
            log.info("Provider payment failure ignored for attempt {}. Because attempt is not eligible", attempt.getId());
            return;
        }

        attempt.markFailed(evidence);
        paymentAttemptRepository.save(attempt);

        paymentEventPublisher.publish(new PaymentFailedEvent(attempt));
    }

    private void applySuccess(Payment payment, PaymentAttempt attempt, ProviderPaymentEvidence evidence) {
        if (attempt.canAutoConfirm(evidence.receivedAt())) {
            confirmPayment(payment, attempt, evidence);
        } else {
            requireReconciliation(payment, attempt, evidence);
        }
    }

    private void confirmPayment(Payment payment, PaymentAttempt attempt, ProviderPaymentEvidence evidence) {
        attempt.markPaid(evidence);
        paymentAttemptRepository.save(attempt);

        payment.markPaid(evidence.receivedAt());
        paymentRepository.save(payment);

        paymentEventPublisher.publish(new PaymentConfirmedEvent(attempt));
    }

    private void requireReconciliation(Payment payment, PaymentAttempt attempt, ProviderPaymentEvidence evidence) {
        PaymentReconciliationReason reason = determineReconciliationReason(payment, attempt, evidence.receivedAt());

        attempt.markReconciliationRequired(evidence, reason);
        paymentAttemptRepository.save(attempt);

        payment.markReconciliationRequired(evidence.receivedAt());
        paymentRepository.save(payment);

        paymentEventPublisher.publish(new PaymentReconciliationRequiredEvent(attempt, reason));
    }

    private PaymentReconciliationReason determineReconciliationReason(Payment payment,
                                                                       PaymentAttempt attempt,
                                                                       Instant receivedAt) {
        if (attempt.isPendingPastDue(receivedAt) || attempt.isExpiredStatus()) {
            return PaymentReconciliationReason.LATE_SUCCESS_AFTER_EXPIRED;
        }
        return payment.reconciliationReasonForSuccessfulProviderResult(receivedAt);
    }

    private ProviderPaymentEvidence buildEvidence(ProcessPaymentResultCommand command) {
        return new ProviderPaymentEvidence(
                command.responseCode(),
                command.providerStatusCode(),
                command.providerTransactionId(),
                command.rawPayload(),
                command.receivedAt(),
                command.amount()
        );
    }

    private boolean isAmountMismatched(PaymentAttempt attempt, BigDecimal amount) {
        return attempt.getAmount().compareTo(amount) != 0;
    }

    private PaymentAttempt getLockedAttempt(PaymentAttempt attemptReference) {
        return paymentAttemptRepository.findLockedById(attemptReference.getId())
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private Payment getLockedPayment(PaymentAttempt attemptReference) {
        return paymentRepository.findLockedById(attemptReference.getPaymentId())
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }

    private PaymentAttempt getUnlockedAttempt(String providerReference) {
        return paymentAttemptRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new BusinessException(ResponseCode.PAYMENT_NOT_FOUND));
    }
}
