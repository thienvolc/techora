package com.techora.order.application.service;

import com.techora.order.application.command.PlaceOrderCommand;
import com.techora.order.application.result.OrderSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderCheckoutService {
    private final OrderPlacementService orderPlacementService;
    private final OrderReservationService orderReservationService;

    public OrderSnapshot place(PlaceOrderCommand command) {
        return orderPlacementService.place(command);
    }

    public OrderSnapshot markStockReserved(UUID orderId) {
        return orderReservationService.markStockReserved(orderId);
    }
}
