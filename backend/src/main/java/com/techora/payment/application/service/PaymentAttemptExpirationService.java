package com.techora.payment.application.service;

import com.techora.payment.application.port.persistence.PaymentAttemptRepository;
import com.techora.payment.domain.entity.PaymentAttempt;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentAttemptExpirationService {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final Clock clock;

    @Value("${payment.attempt-expiration-job.batch-size:${payment.expiration-job.batch-size:100}}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${payment.attempt-expiration-job.fixed-delay-ms:${payment.expiration-job.fixed-delay-ms:60000}}")
    @SchedulerLock(
            name = "paymentAttemptExpirationService.expirePendingAttempts",
            lockAtMostFor = "${payment.attempt-expiration-job.lock-at-most-for:PT5M}"
    )
    @Transactional
    public int expirePendingAttempts() {
        Instant now = Instant.now(clock);
        List<PaymentAttempt> expiredAttempts = paymentAttemptRepository.findExpiredPendingForUpdate(
                now,
                resolvedBatchSize()
        );

        expiredAttempts.forEach(attempt -> expire(attempt, now));
        return expiredAttempts.size();
    }

    private void expire(PaymentAttempt attempt, Instant now) {
        if (!attempt.markExpired(now)) {
            return;
        }

        paymentAttemptRepository.save(attempt);
    }

    private int resolvedBatchSize() {
        return batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }
}
