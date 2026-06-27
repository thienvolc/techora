package com.techora.common.domain.event;

public interface InternalEventPublisher {
    void publish(InternalEvent event);
}
