package com.techora.inventory.infra.catalog;

import com.techora.catalog.application.port.inventory.CatalogInventoryPort;
import com.techora.catalog.application.port.inventory.CatalogInventoryStock;
import com.techora.inventory.application.result.InventoryStockSnapshot;
import com.techora.inventory.application.service.InventoryItemService;
import com.techora.inventory.application.service.InventoryStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogInventoryPortAdapter implements CatalogInventoryPort {
    private final InventoryItemService inventoryItemService;
    private final InventoryStockQueryService inventoryStockQueryService;

    @Override
    public CatalogInventoryStock initializeProductStock(UUID productId, int quantity) {
        return toStock(inventoryItemService.initializeProductStock(productId, quantity));
    }

    @Override
    public int getQuantityOnHand(UUID productId) {
        return inventoryStockQueryService.getQuantityOnHand(productId);
    }

    @Override
    public int getAvailableQuantity(UUID productId) {
        return inventoryStockQueryService.getAvailableQuantity(productId);
    }

    @Override
    public Map<UUID, Integer> getAvailableQuantities(Collection<UUID> productIds) {
        return inventoryStockQueryService.getAvailableQuantities(productIds);
    }

    private CatalogInventoryStock toStock(InventoryStockSnapshot stock) {
        return new CatalogInventoryStock(
                stock.productId(),
                stock.quantityOnHand(),
                stock.reservedQuantity(),
                stock.availableQuantity(),
                stock.updatedAt()
        );
    }
}
