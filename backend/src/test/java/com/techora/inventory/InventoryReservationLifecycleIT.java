package com.techora.inventory;

import com.techora.inventory.application.command.ReserveInventoryCommand;
import com.techora.inventory.application.command.ReserveInventoryItem;
import com.techora.inventory.application.model.InventoryReservationMismatch;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.application.scheduler.InventoryReservationReconciliationService;
import com.techora.inventory.application.service.InventoryReservationService;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryReservationLifecycleIT extends AbstractIntegrationTest {

    private static final Instant TEST_TIME = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant RESERVATION_EXPIRES_AT = TEST_TIME.plusSeconds(3600);

    @Autowired
    private InventoryReservationService inventoryReservationService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private InventoryReservationRepository reservationRepository;

    @Autowired
    private InventoryReservationReconciliationService reconciliationService;

    @Test
    void duplicateReservationForSameOrderDoesNotReserveStockTwice() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        ReserveInventoryCommand command = reserveInventoryCommand(orderId, productId, 2);
        inventoryItemRepository.save(TestFixtures.inventoryItem(productId, 10));

        inventoryReservationService.reserveOrder(command);
        inventoryReservationService.reserveOrder(command);

        assertThat(inventoryItemRepository.findByProductId(productId).orElseThrow())
                .satisfies(stock -> {
                    assertThat(stock.getQuantityOnHand()).isEqualTo(10);
                    assertThat(stock.getReservedQuantity()).isEqualTo(2);
                    assertThat(stock.availableQuantity()).isEqualTo(8);
                });
        assertThat(reservationRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .singleElement()
                .satisfies(reservation -> {
                    assertThat(reservation.getProductId()).isEqualTo(productId);
                    assertThat(reservation.getQuantity()).isEqualTo(2);
                    assertThat(reservation.getStatus()).isEqualTo(InventoryReservationStatus.RESERVED);
                });
    }

    @Test
    void reconciliationDetectsReservedQuantityMismatch() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        inventoryItemRepository.save(inventoryItem(productId, 10, 5));
        reservationRepository.save(TestFixtures.reservedInventoryReservation(
                orderId,
                productId,
                2,
                RESERVATION_EXPIRES_AT
        ));

        List<InventoryReservationMismatch> mismatches = reconciliationService.findMismatches();
        int mismatchCount = reconciliationService.reconcileReservedQuantityInvariant();

        assertThat(mismatches)
                .singleElement()
                .satisfies(mismatch -> {
                    assertThat(mismatch.productId()).isEqualTo(productId);
                    assertThat(mismatch.stockReservedQuantity()).isEqualTo(5);
                    assertThat(mismatch.reservationReservedQuantity()).isEqualTo(2);
                    assertThat(mismatch.difference()).isEqualTo(3);
                });
        assertThat(mismatchCount).isEqualTo(1);
    }

    private ReserveInventoryCommand reserveInventoryCommand(UUID orderId, UUID productId, int quantity) {
        return new ReserveInventoryCommand(
                orderId,
                RESERVATION_EXPIRES_AT,
                List.of(new ReserveInventoryItem(productId, quantity))
        );
    }

    private InventoryItemEntity inventoryItem(UUID productId, int quantityOnHand, int reservedQuantity) {
        return InventoryItemEntity.builder()
                .productId(productId)
                .quantityOnHand(quantityOnHand)
                .reservedQuantity(reservedQuantity)
                .createdAt(TEST_TIME)
                .updatedAt(TEST_TIME)
                .build();
    }
}
