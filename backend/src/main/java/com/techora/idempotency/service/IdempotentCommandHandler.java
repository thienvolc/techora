package com.techora.idempotency.service;

@FunctionalInterface
public interface IdempotentCommandHandler<T> {
    T handle();
}
