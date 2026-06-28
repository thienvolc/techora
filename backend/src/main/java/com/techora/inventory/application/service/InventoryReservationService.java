package com.techora.inventory.application.service;

import com.techora.common.domain.event.InternalEventPublisher;
import com.techora.inventory.application.command.ReserveInventoryCommand;
import com.techora.inventory.application.command.ReserveInventoryItem;
import com.techora.inventory.application.mapper.ReservationMapper;
import com.techora.inventory.application.repository.InventoryReservationRepository;
import com.techora.inventory.application.result.InventoryStockSnapshot;
import com.techora.inventory.domain.entity.InventoryReservationEntity;
import com.techora.inventory.domain.event.StockReducedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryReservationService {
    private final InventoryReservationRepository reservationRepository;
    private final InventoryItemService inventoryItemService;
    private final ReservationMapper reservationMapper;
    private final InternalEventPublisher internalEventPublisher;
    private final Clock clock;

    @Transactional
    public void reserveOrder(ReserveInventoryCommand command) {
        if (reservationExists(command.orderId())) {
            return;
        }
        reserveAllItems(command);
    }

    private boolean reservationExists(UUID orderId) {
        return !reservationRepository.findLockedByOrderId(orderId).isEmpty();
    }

    private void reserveAllItems(ReserveInventoryCommand command) {
        // Lock inventory rows in deterministic product order to reduce cross-order deadlocks.
        command.items().stream()
                .sorted(Comparator.comparing(ReserveInventoryItem::productId))
                .forEach(item -> reserveItem(item, command));
    }

    private void reserveItem(ReserveInventoryItem item, ReserveInventoryCommand command) {
        createReservation(item, command);
        reserveProductAvailableQuantity(item);
    }

    private void createReservation(ReserveInventoryItem item, ReserveInventoryCommand command) {
        var reservation = reservationMapper.toModel(item, command, now());
        reservationRepository.saveAndFlush(reservation);
    }

    private void reserveProductAvailableQuantity(ReserveInventoryItem item) {
        inventoryItemService.reserve(item.productId(), item.quantity());
    }


    @Transactional
    public void confirmOrder(UUID orderId) {
        var reservations = getReservedReservationsOfOrder(orderId);
        confirmAllReservations(reservations);
    }

    private void confirmAllReservations(List<InventoryReservationEntity> reservations) {
        reservations.forEach(this::confirmReservation);
        reservationRepository.saveAll(reservations);
    }

    private void confirmReservation(InventoryReservationEntity reservation) {
        InventoryStockSnapshot stock = confirmProductReservedQuantity(reservation);
        reservation.markConfirmed();
        publishStockReducedEvent(stock, reservation.getQuantity());
    }

    private InventoryStockSnapshot confirmProductReservedQuantity(InventoryReservationEntity reservation) {
        return inventoryItemService.confirmReserved(reservation.getProductId(), reservation.getQuantity());
    }

    private void publishStockReducedEvent(InventoryStockSnapshot stock, int quantity) {
        internalEventPublisher.publish(StockReducedEvent.of(
                stock.productId(),
                quantity,
                stock.quantityOnHand(),
                stock.updatedAt()));
    }


    @Transactional
    public void release(UUID orderId) {
        var reservations = getReservedReservationsOfOrder(orderId);
        releaseAllReservations(reservations);
    }

    private void releaseAllReservations(List<InventoryReservationEntity> reservations) {
        reservations.forEach(this::releaseReservation);
        reservationRepository.saveAll(reservations);
    }

    private void releaseReservation(InventoryReservationEntity reservation) {
        releaseProductReservedQuantity(reservation);
        reservation.markRelease();
    }

    private void releaseProductReservedQuantity(InventoryReservationEntity reservation) {
        inventoryItemService.releaseReserved(reservation.getProductId(), reservation.getQuantity());
    }


    private List<InventoryReservationEntity> getReservedReservationsOfOrder(UUID orderId) {
        return reservationRepository.findLockedByOrderId(orderId).stream()
                .filter(InventoryReservationEntity::isReserved)
                .toList();
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
