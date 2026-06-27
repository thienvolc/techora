package com.techora.outbox.service;

import com.techora.outbox.dto.OutboxEventRecord;
import com.techora.outbox.port.OutboxEventPort;
import com.techora.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxEventService implements OutboxEventPort {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    @Override
    public void append(OutboxEventRecord record) {
        outboxEventRepository.save(outboxEventFactory.create(record));
    }
}
