package com.techora.payment.infra.outbox;

import com.techora.outbox.constant.OutboxAggregateType;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.port.OutboxEventPort;
import com.techora.payment.domain.event.PaymentConfirmedEvent;
import com.techora.payment.domain.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentOutboxEventPublisher {

    private static final String PAYMENT_ID = "paymentId";
    private static final String ORDER_ID = "orderId";
    private static final String USER_ID = "userId";
    private static final String PAYMENT_STATUS = "paymentStatus";
    private static final String AMOUNT = "amount";

    private final OutboxEventPort outboxEventPort;

    public void recordConfirmed(PaymentConfirmedEvent event) {
        Map<String, Object> attributes = Map.of(
                PAYMENT_ID, event.paymentId(),
                ORDER_ID, event.orderId(),
                USER_ID, event.userId(),
                PAYMENT_STATUS, event.status().name(),
                AMOUNT, event.amount()
        );
        var record = new OutboxEventRecord(
                OutboxAggregateType.PAYMENT,
                event.paymentId(),
                OutboxEventType.PAYMENT_CONFIRMED,
                attributes
        );
        outboxEventPort.append(record);
    }

    public void recordFailed(PaymentFailedEvent event) {
        Map<String, Object> attributes = Map.of(
                PAYMENT_ID, event.paymentId(),
                ORDER_ID, event.orderId(),
                USER_ID, event.userId(),
                PAYMENT_STATUS, event.status().name(),
                AMOUNT, event.amount()
        );
        var record = new OutboxEventRecord(
                OutboxAggregateType.PAYMENT,
                event.paymentId(),
                OutboxEventType.PAYMENT_FAILED,
                attributes
        );
        outboxEventPort.append(record);
    }
}
