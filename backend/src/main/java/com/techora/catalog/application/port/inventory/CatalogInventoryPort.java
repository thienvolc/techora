package com.techora.catalog.application.port.inventory;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface CatalogInventoryPort {
    CatalogInventoryStock initializeProductStock(UUID productId, int quantity);

    int getQuantityOnHand(UUID productId);

    int getAvailableQuantity(UUID productId);

    Map<UUID, Integer> getAvailableQuantities(Collection<UUID> productIds);
}
