package com.techora.outbox.port;

import com.techora.outbox.dto.OutboxEventRecord;

public interface OutboxEventPort {

    void append(OutboxEventRecord record);
}
