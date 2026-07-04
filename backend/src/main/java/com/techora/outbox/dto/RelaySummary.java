package com.techora.outbox.dto;

import com.techora.outbox.service.OutboxBatchPublisher;
import com.techora.outbox.service.OutboxBatchPublisher.PublishResult;

import java.util.List;

public record RelaySummary(
        int attempted,
        int succeeded,
        int failed
) {

    public static RelaySummary empty() {
        return new RelaySummary(0, 0, 0);
    }

    public boolean hasProcessedAny() {
        return attempted > 0;
    }

    public boolean isPartial(int expectedBatchSize) {
        return attempted < expectedBatchSize;
    }

    public RelaySummary merge(RelaySummary summary) {
        return new RelaySummary(
                this.attempted + summary.attempted,
                this.succeeded + summary.succeeded,
                this.failed + summary.failed
        );
    }

    public static RelaySummary from(List<PublishResult> results) {
        int total = results.size();
        int succeeded = (int) results.stream()
                .filter(PublishResult::isSuccess)
                .count();
        int failed = total - succeeded;

        return new RelaySummary(total, succeeded, failed);
    }
}
