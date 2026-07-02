package com.techora.cart.dto.order;

import java.util.List;
import java.util.UUID;

public record CartSnapshot(
        UUID userId,
        String username,
        List<CartItemSnapshot> items
) {
    public CartSnapshot {
        items = List.copyOf(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
