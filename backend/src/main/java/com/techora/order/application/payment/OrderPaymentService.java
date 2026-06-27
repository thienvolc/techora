package com.techora.order.application.payment;

import com.techora.order.application.actor.OrderActor;
import com.techora.order.application.command.UpdateOrderStatusCommand;
import com.techora.order.application.eventpublisher.OrderStatusChange;
import com.techora.order.application.eventpublisher.OrderStatusChangedEventPublisher;
import com.techora.order.application.service.OrderStatusUpdater;
import com.techora.order.domain.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderPaymentService {
    private final OrderStatusChangedEventPublisher statusChangedEventPublisher;
    private final OrderStatusUpdater orderStatusUpdater;

    public PaymentPreparedOrder preparePayment(UUID orderId) {
        OrderStatusChange statusChange = updatePaymentPendingStatus(orderId);
        return PaymentPreparedOrder.from(statusChange.order());
    }

    public void confirmPayment(UUID orderId, String providerName) {
        OrderActor actor = providerActor(providerName);
        updatePaidStatus(orderId, actor);
    }

    public void markPaymentFailedAndCancelOrder(UUID orderId, String providerName) {
        OrderActor actor = providerActor(providerName);
        updatePaymentFailedStatus(orderId, actor);
        cancelAfterPaymentFailure(orderId, actor);
    }

    private OrderStatusChange updatePaymentPendingStatus(UUID orderId) {
        return updateStatus(orderId, OrderStatus.PAYMENT_PENDING, OrderActor.system());
    }

    private void cancelAfterPaymentFailure(UUID orderId, OrderActor actor) {
        updateStatus(orderId, OrderStatus.CANCELLED, actor);
    }

    private void updatePaymentFailedStatus(UUID orderId, OrderActor actor) {
        updateStatus(orderId, OrderStatus.PAYMENT_FAILED, actor);
    }

    private void updatePaidStatus(UUID orderId, OrderActor actor) {
        updateStatus(orderId, OrderStatus.PAID, actor);
    }

    private OrderStatusChange updateStatus(UUID orderId,
                                           OrderStatus newStatus,
                                           OrderActor actor) {

        return orderStatusUpdater.update(
                new UpdateOrderStatusCommand(
                        orderId,
                        newStatus,
                        actor
                )
        );
    }

    private OrderActor providerActor(String providerName) {
        return OrderActor.provider(providerName);
    }
}
