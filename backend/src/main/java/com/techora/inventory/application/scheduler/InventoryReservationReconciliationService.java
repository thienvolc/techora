package com.techora.inventory.application.scheduler;

import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.application.model.InventoryReservationMismatch;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationReconciliationService {
    private static final String RUN_METRIC = "techora.inventory.reservation_reconciliation.run";
    private static final String MISMATCH_METRIC = "techora.inventory.reservation_reconciliation.mismatch";

    private final InventoryReservationRepository inventoryReservationRepository;
    private final MeterRegistry meterRegistry;


    @Scheduled(fixedDelayString = "${inventory.reservation-reconciliation-job.fixed-delay-ms:300000}")
    @SchedulerLock(
            name = "inventoryReservationReconciliationService.reconcileReservedQuantityInvariant",
            lockAtMostFor = "${inventory.reservation-reconciliation-job.lock-at-most-for:PT10M}"
    )
    @Transactional(readOnly = true)
    public int reconcileReservedQuantityInvariant() {
        List<InventoryReservationMismatch> mismatches = findMismatches();
        recordMetrics(mismatches);
        logMismatches(mismatches);
        return mismatches.size();
    }

    @Transactional(readOnly = true)
    public List<InventoryReservationMismatch> findMismatches() {
        return inventoryReservationRepository.findReservationMismatches().stream()
                .map(InventoryReservationMismatch::from)
                .toList();
    }

    private void recordMetrics(List<InventoryReservationMismatch> mismatches) {
        meterRegistry.counter(RUN_METRIC).increment();
        if (!mismatches.isEmpty()) {
            meterRegistry.counter(MISMATCH_METRIC).increment(mismatches.size());
        }
    }

    private void logMismatches(List<InventoryReservationMismatch> mismatches) {
        if (mismatches.isEmpty()) {
            return;
        }

        log.warn("Inventory reservation invariant mismatch detected. count={}", mismatches.size());
        mismatches.forEach(mismatch -> log.warn(
                "Inventory reservation mismatch. productId={}, stockReservedQuantity={}, reservationReservedQuantity={}, difference={}",
                mismatch.productId(),
                mismatch.stockReservedQuantity(),
                mismatch.reservationReservedQuantity(),
                mismatch.difference()
        ));
    }
}
