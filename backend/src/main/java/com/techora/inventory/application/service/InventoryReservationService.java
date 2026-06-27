package com.techora.inventory.application.service;

import com.techora.catalog.dto.ProductSnapshot;
import com.techora.catalog.service.ProductAvailabilityService;
import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.inventory.application.command.ReserveInventoryCommand;
import com.techora.inventory.application.command.ReserveInventoryItem;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.entity.InventoryReservationStatus;
import com.techora.inventory.domain.event.StockReducedEvent;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.application.result.InventoryStockSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryReservationService {

    private static final int RESERVATION_EXPIRATION_MINUTES = 30;
    private final InventoryReservationRepository repository;
    private final InventoryItemService inventoryItemService;
    private final ProductAvailabilityService productAvailabilityService;
    private final InternalEventPublisher internalEventPublisher;

    public void reserve(ReserveInventoryCommand command) {
        command.items()
                .forEach(item -> reserve(command.orderId(), item));
    }

    public void confirm(UUID orderId) {
        var reservations = getReservationsOfOrder(orderId);
        reservations.forEach(this::confirm);
        repository.saveAll(reservations);
    }

    public void release(UUID orderId) {
        var reservations = getReservationsOfOrder(orderId);
        reservations.forEach(this::release);
        repository.saveAll(reservations);
    }

    @Scheduled(fixedDelay = RESERVATION_EXPIRATION_MINUTES * 60 * 1000)
    public int expireAbandonedReservations() {
        List<InventoryReservationEntity> expiredReservations = repository.findByStatusAndExpiresAtBefore(
                InventoryReservationStatus.RESERVED, Instant.now());
        expiredReservations.forEach(this::expire);
        repository.saveAll(expiredReservations);
        return expiredReservations.size();
    }

    private void reserve(UUID orderId, ReserveInventoryItem item) {
        ProductSnapshot product = getAvailableProduct(item.productId());
        inventoryItemService.reserve(product.id(), item.quantity());
        var reservation = buildReservation(orderId, product.id(), item.quantity());
        repository.save(reservation);
    }

    private List<InventoryReservationEntity> getReservationsOfOrder(UUID orderId) {
        return repository.findLockedByOrderId(orderId).stream()
                .filter(InventoryReservationEntity::isReserved)
                .toList();
    }

    private ProductSnapshot getAvailableProduct(UUID productId) {
        return productAvailabilityService.getLockedAvailableSnapshotOrThrow(productId);
    }

    private void confirm(InventoryReservationEntity reservation) {
        int quantity = reservation.getQuantity();
        UUID productId = reservation.getProductId();

        InventoryStockSnapshot stock = inventoryItemService.confirmReserved(productId, quantity);
        reservation.markConfirmed();
        internalEventPublisher.publish(StockReducedEvent.of(
                stock.productId(),
                quantity,
                stock.quantityOnHand(),
                stock.updatedAt()));
    }

    private void release(InventoryReservationEntity reservation) {
        inventoryItemService.releaseReserved(
                reservation.getProductId(),
                reservation.getQuantity());
        reservation.markRelease();
    }

    private void expire(InventoryReservationEntity reservation) {
        inventoryItemService.releaseReserved(
                reservation.getProductId(),
                reservation.getQuantity());
        reservation.markExpired();
    }

    private InventoryReservationEntity buildReservation(UUID orderId,
                                                        UUID productId,
                                                        int quantity) {

        Instant now = Instant.now();
        return InventoryReservationEntity.builder()
                .orderId(orderId)
                .productId(productId)
                .quantity(quantity)
                .status(InventoryReservationStatus.RESERVED)
                .expiresAt(now.plus(RESERVATION_EXPIRATION_MINUTES, ChronoUnit.MINUTES))
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
