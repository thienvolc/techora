package com.techora.order.application.eventpublisher;

import com.techora.order.application.actor.OrderActor;
import com.techora.order.domain.entity.Order;
import com.techora.order.domain.entity.OrderStatus;

public record OrderStatusChange(
        Order order,
        OrderStatus oldStatus,
        OrderStatus newStatus,
        OrderActor actor
) {

}
