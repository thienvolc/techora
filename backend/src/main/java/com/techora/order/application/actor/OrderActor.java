package com.techora.order.application.actor;

import com.techora.order.domain.event.OrderEventActorType;

import java.util.UUID;

public record OrderActor(
        UUID actorId,
        String actorName,
        OrderEventActorType actorType
) {
    public static OrderActor system() {
        return new OrderActor(null, null, OrderEventActorType.SYSTEM);
    }

    public static OrderActor provider(String providerName) {
        return new OrderActor(null, providerName, OrderEventActorType.SYSTEM);
    }

    public static OrderActor admin(UUID adminId, String adminName) {
        return new OrderActor(adminId, adminName, OrderEventActorType.ADMIN);
    }
}
