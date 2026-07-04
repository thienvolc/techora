package com.techora.order.application.service.payment;

import com.techora.order.application.actor.OrderActor;
import com.techora.order.application.port.payment.PendingPaymentExpirationPort;
import com.techora.order.application.port.payment.PendingPaymentExpirationResult;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.application.service.OrderStatusUpdater;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
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
public class OrderPaymentWindowExpirationService {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final OrderStatusUpdater orderStatusUpdater;
    private final PendingPaymentExpirationPort paymentExpirationPort;
    private final Clock clock;

    @Value("${order.payment-window-expiration-job.batch-size:${payment.expiration-job.batch-size:100}}")
    private int batchSize;

    // TODO: We use shedlock for expire orders job,
    //  its' mean only one running instance at the same time
    //  For scaling, we should use the same strategy in outbox module
    //  which currently uses the lock machenism and can work on multiple instance
    @Scheduled(fixedDelayString = "${order.payment-window-expiration-job.fixed-delay-ms:${payment.expiration-job.fixed-delay-ms:60000}}")
    @SchedulerLock(
            name = "orderPaymentWindowExpirationService.expireUnpaidOrders",
            lockAtMostFor = "${order.payment-window-expiration-job.lock-at-most-for:PT5M}"
    )
    @Transactional
    public int expireUnpaidOrders() {
        List<Order> expiredOrders = orderRepository.findExpiredPaymentPendingForUpdate(
                Instant.now(clock),
                resolvedBatchSize()
        );

        return (int) expiredOrders.stream()
                .filter(this::expire)
                .count();
    }

    private boolean expire(Order order) {
        PendingPaymentExpirationResult result =
                paymentExpirationPort.expirePendingPaymentForOrderTimeout(order.getId());
        if (result != PendingPaymentExpirationResult.EXPIRED) {
            return false;
        }

        orderStatusUpdater.update(order, OrderStatus.CANCELLED, OrderActor.system());
        return true;
    }

    private int resolvedBatchSize() {
        return batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }
}
