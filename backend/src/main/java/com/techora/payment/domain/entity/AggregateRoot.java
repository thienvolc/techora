package com.techora.payment.domain.entity;

import lombok.Getter;

@Getter
public abstract class AggregateRoot<ID> {

    private final ID id;

    protected AggregateRoot(ID id) {
        this.id = id;
    }
}
