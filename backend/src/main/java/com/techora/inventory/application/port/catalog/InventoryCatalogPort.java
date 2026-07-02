package com.techora.inventory.application.port.catalog;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InventoryCatalogPort {
    InventoryCatalogProduct getProduct(UUID productId);

    List<InventoryCatalogProduct> getProducts(Collection<UUID> productIds);

    List<InventoryCatalogProduct> getAllProducts();
}
