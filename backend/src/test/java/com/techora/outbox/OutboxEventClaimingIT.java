package com.techora.outbox;

import com.techora.outbox.constant.OutboxEventStatus;
import com.techora.outbox.constant.OutboxEventType;
import com.techora.outbox.entity.OutboxEventEntity;
import com.techora.outbox.repository.OutboxEventBulkClaimer;
import com.techora.outbox.repository.OutboxEventRepository;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventClaimingIT extends AbstractIntegrationTest {

    private static final List<OutboxEventType> PAYMENT_CONFIRMED_EVENTS =
            List.of(OutboxEventType.PAYMENT_CONFIRMED);

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventBulkClaimer bulkClaimer;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void claimReadyEventsByTypesSkipsRowsLockedByAnotherTransaction() throws Exception {
        List<UUID> readyEventIds = seedReadyPaymentEventIds(3);

        ClaimRaceResult result = runClaimRace(
                new ClaimRequest("worker-a", 2),
                new ClaimRequest("worker-b", 2)
        );

        assertThat(result.firstClaimedIds()).hasSize(2);
        assertThat(result.secondClaimedIds()).hasSize(1);
        assertThat(result.allClaimedIds())
                .containsExactlyInAnyOrderElementsOf(readyEventIds);

        assertClaimedBy(result.firstClaimedIds(), "worker-a");
        assertClaimedBy(result.secondClaimedIds(), "worker-b");
    }

    private ClaimRaceResult runClaimRace(ClaimRequest firstWorker,
                                         ClaimRequest secondWorker) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstTransactionClaimed = new CountDownLatch(1);
        CountDownLatch allowFirstTransactionToCommit = new CountDownLatch(1);

        try {
            Future<List<UUID>> firstClaim = executor.submit(() -> claimAndHoldTransaction(
                    firstWorker,
                    firstTransactionClaimed,
                    allowFirstTransactionToCommit
            ));
            waitUntilFirstWorkerHoldsLocks(firstTransactionClaimed, firstClaim);

            Future<List<UUID>> secondClaim = executor.submit(() -> claimInTransaction(secondWorker));
            List<UUID> secondClaimedIds = secondClaim.get(5, TimeUnit.SECONDS);

            allowFirstTransactionToCommit.countDown();
            List<UUID> firstClaimedIds = firstClaim.get(5, TimeUnit.SECONDS);

            return new ClaimRaceResult(firstClaimedIds, secondClaimedIds);
        } finally {
            allowFirstTransactionToCommit.countDown();
            executor.shutdownNow();
        }
    }

    private List<UUID> claimAndHoldTransaction(ClaimRequest request,
                                               CountDownLatch claimed,
                                               CountDownLatch allowCommit) {

        return transactionTemplate().execute(status -> {
            List<UUID> claimedIds = claimIds(request);
            claimed.countDown();
            await(allowCommit);
            return claimedIds;
        });
    }

    private void waitUntilFirstWorkerHoldsLocks(CountDownLatch firstTransactionClaimed,
                                                Future<List<UUID>> firstClaim) throws Exception {
        if (firstTransactionClaimed.await(10, TimeUnit.SECONDS)) {
            return;
        }
        if (firstClaim.isDone()) {
            firstClaim.get(1, TimeUnit.SECONDS);
        }
        throw new AssertionError("First outbox claim did not finish within timeout");
    }

    private List<UUID> claimInTransaction(ClaimRequest request) {
        return transactionTemplate().execute(status -> claimIds(request));
    }

    private List<UUID> claimIds(ClaimRequest request) {
        return bulkClaimer.claimReadyEventsByTypes(
                        PAYMENT_CONFIRMED_EVENTS,
                        request.workerId(),
                        request.batchSize()
                )
                .stream()
                .map(OutboxEventEntity::getId)
                .toList();
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for outbox claim test coordination", ex);
        }
    }

    private List<UUID> seedReadyPaymentEventIds(int count) {
        Instant now = Instant.now();
        List<OutboxEventEntity> events = java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> {
                    OutboxEventEntity event = TestFixtures.pendingPaymentOutboxEvent(
                            UUID.randomUUID(),
                            OutboxEventType.PAYMENT_CONFIRMED
                    );
                    event.setCreatedAt(now.plusMillis(index));
                    event.setUpdatedAt(now.plusMillis(index));
                    event.setNextAttemptAt(now.minusSeconds(1));
                    return event;
                })
                .toList();

        return outboxEventRepository.saveAll(events).stream()
                .map(OutboxEventEntity::getId)
                .toList();
    }

    private void assertClaimedBy(List<UUID> claimedIds, String lockedBy) {
        List<OutboxEventEntity> claimedRows = outboxEventRepository.findAllById(claimedIds);

        assertThat(claimedRows)
                .hasSize(claimedIds.size())
                .allSatisfy(event -> {
                    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
                    assertThat(event.getLockedBy()).isEqualTo(lockedBy);
                    assertThat(event.getLockedAt()).isNotNull();
                });
    }

    private record ClaimRequest(String workerId, int batchSize) {
    }

    private record ClaimRaceResult(
            List<UUID> firstClaimedIds,
            List<UUID> secondClaimedIds
    ) {
        List<UUID> allClaimedIds() {
            return Stream.concat(firstClaimedIds.stream(), secondClaimedIds.stream())
                    .toList();
        }
    }
}
