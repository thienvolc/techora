package com.techora.checkout.application;

import com.techora.cart.dto.checkout.CartCheckoutItem;
import com.techora.cart.dto.checkout.CartCheckoutSnapshot;
import com.techora.order.application.service.OrderPrice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderPricingService {

    public OrderPrice calculate(CartCheckoutSnapshot cart) {
        Map<UUID, BigDecimal> subtotalByProductId =
                cart.items().stream()
                        .collect(Collectors.toMap(
                                CartCheckoutItem::productId,
                                CartCheckoutItem::subtotal,
                                BigDecimal::add));

        BigDecimal total =
                subtotalByProductId.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderPrice(total, subtotalByProductId);
    }
}
