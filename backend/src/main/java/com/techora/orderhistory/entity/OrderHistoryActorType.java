package com.techora.orderhistory.entity;

import com.techora.order.domain.event.OrderEventActorType;

public enum OrderHistoryActorType {
    USER,
    ADMIN,
    SYSTEM;

    public static OrderHistoryActorType of(OrderEventActorType orderEventActorType) {
        return switch (orderEventActorType) {
            case OrderEventActorType.USER -> USER;
            case OrderEventActorType.ADMIN -> ADMIN;
            case OrderEventActorType.SYSTEM -> SYSTEM;
        };
    }
}
