package com.techora.cart.dto.checkout;

import java.util.List;
import java.util.UUID;

public record CartCheckoutSnapshot(
        UUID userId,
        String username,
        List<CartCheckoutItem> items
) {
    public CartCheckoutSnapshot {
        items = List.copyOf(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
