package com.techora.common.infra.event;

import com.techora.common.domain.event.InternalEvent;
import com.techora.common.domain.event.InternalEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringInternalEventPublisher implements InternalEventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(InternalEvent event) {
        publisher.publishEvent(event);
    }
}
