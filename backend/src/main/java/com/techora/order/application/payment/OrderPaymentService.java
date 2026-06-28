package com.techora.order.application.payment;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.order.application.actor.OrderActor;
import com.techora.order.application.eventpublisher.OrderStatusChange;
import com.techora.order.application.port.persistence.OrderRepository;
import com.techora.order.application.security.OrderPermissionService;
import com.techora.order.application.service.OrderStatusUpdater;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderPaymentService {
    private final OrderPermissionService orderPermissionService;
    private final OrderRepository orderRepository;
    private final OrderStatusUpdater orderStatusUpdater;

    public PaymentPreparedOrder preparePayment(UUID userId, UUID orderId) {
        Order order = orderPermissionService.getOwnedOrderForUpdate(userId, orderId);
        if (order.getStatus() == OrderStatus.PAYMENT_PENDING) {
            return toPreparedOrder(order);
        }

        OrderStatusChange statusChange = updatePaymentPendingStatus(order);
        return toPreparedOrder(statusChange.order());
    }

    public OrderPaymentConfirmationResult confirmPayment(UUID orderId, String providerName) {
        Order order = getOrder(orderId);
        if (order.getStatus() == OrderStatus.PAID) {
            return OrderPaymentConfirmationResult.ALREADY_PAID;
        }

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            return OrderPaymentConfirmationResult.NOT_PAYABLE;
        }

        OrderActor actor = providerActor(providerName);
        orderStatusUpdater.update(order, OrderStatus.PAID, actor);
        return OrderPaymentConfirmationResult.CONFIRMED;
    }

    private OrderStatusChange updatePaymentPendingStatus(Order order) {
        return orderStatusUpdater.update(order, OrderStatus.PAYMENT_PENDING, OrderActor.system());
    }

    private PaymentPreparedOrder toPreparedOrder(Order order) {
        return PaymentPreparedOrder.from(order);
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findLockedWithItemsById(orderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.ORDER_NOT_FOUND));
    }

    private OrderActor providerActor(String providerName) {
        return OrderActor.provider(providerName);
    }
}
