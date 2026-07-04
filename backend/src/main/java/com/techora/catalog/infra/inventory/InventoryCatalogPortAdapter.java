package com.techora.catalog.infra.inventory;

import com.techora.catalog.application.model.CatalogCategorySnapshot;
import com.techora.catalog.application.model.CatalogProductSnapshot;
import com.techora.catalog.application.service.ProductCatalogQueryService;
import com.techora.inventory.application.port.catalog.InventoryCatalogCategory;
import com.techora.inventory.application.port.catalog.InventoryCatalogPort;
import com.techora.inventory.application.port.catalog.InventoryCatalogProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryCatalogPortAdapter implements InventoryCatalogPort {
    private final ProductCatalogQueryService productCatalogQueryService;

    @Override
    public InventoryCatalogProduct getProduct(UUID productId) {
        return toProduct(productCatalogQueryService.getProduct(productId));
    }

    @Override
    public List<InventoryCatalogProduct> getProducts(Collection<UUID> productIds) {
        return productCatalogQueryService.getProducts(productIds).stream()
                .map(this::toProduct)
                .toList();
    }

    @Override
    public List<InventoryCatalogProduct> getAllProducts() {
        return productCatalogQueryService.getAllProducts().stream()
                .map(this::toProduct)
                .toList();
    }

    private InventoryCatalogProduct toProduct(CatalogProductSnapshot product) {
        return new InventoryCatalogProduct(
                product.id(),
                product.name(),
                product.sku(),
                product.slug(),
                product.description(),
                product.price(),
                product.status(),
                toCategory(product.category()),
                product.createdAt(),
                product.updatedAt()
        );
    }

    private InventoryCatalogCategory toCategory(CatalogCategorySnapshot category) {
        return new InventoryCatalogCategory(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.active(),
                category.createdAt(),
                category.updatedAt()
        );
    }
}
