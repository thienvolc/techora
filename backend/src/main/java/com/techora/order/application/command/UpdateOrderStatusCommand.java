package com.techora.order.application.command;

import com.techora.order.application.actor.OrderActor;
import com.techora.order.domain.entity.OrderStatus;

import java.util.UUID;

public record UpdateOrderStatusCommand(
        UUID orderId,
        OrderStatus nextStatus,
        OrderActor actor
) {
    public static UpdateOrderStatusCommand fromAdmin(UUID orderId,
                                                     OrderStatus nextStatus,
                                                     UUID actorId,
                                                     String actorName) {
        OrderActor actor = OrderActor.admin(actorId, actorName);
        return new UpdateOrderStatusCommand(orderId, nextStatus, actor);
    }
}
