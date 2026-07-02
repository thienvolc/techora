package com.techora.order.application.service;

import com.techora.cart.dto.order.CartItemSnapshot;
import com.techora.cart.dto.order.CartSnapshot;
import com.techora.order.application.model.OrderPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderPricingService {

    public OrderPrice calculate(CartSnapshot cart) {
        Map<UUID, BigDecimal> subtotalByProductId =
                cart.items().stream()
                        .collect(Collectors.toMap(
                                CartItemSnapshot::productId,
                                CartItemSnapshot::subtotal,
                                BigDecimal::add));

        BigDecimal total =
                subtotalByProductId.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderPrice(total, subtotalByProductId);
    }
}
