package com.techora.order.application.command;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.order.application.actor.OrderActor;
import com.techora.order.domain.entity.OrderStatus;

import java.util.Locale;
import java.util.UUID;

public record UpdateOrderStatusCommand(
        UUID orderId,
        OrderStatus nextStatus,
        OrderActor actor
) {
    public static UpdateOrderStatusCommand fromAdmin(UUID orderId,
                                                     String nextStatus,
                                                     UUID actorId,
                                                     String actorName) {
        OrderActor actor = OrderActor.admin(actorId, actorName);
        return new UpdateOrderStatusCommand(orderId, parseStatus(nextStatus), actor);
    }

    private static OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new BusinessException(ResponseCode.INVALID_ORDER_STATUS_TRANSITION);
        }
    }
}
