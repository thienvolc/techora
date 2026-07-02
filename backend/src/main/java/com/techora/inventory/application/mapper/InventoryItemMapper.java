package com.techora.inventory.application.mapper;

import com.techora.common.application.dto.response.PageResponse;
import com.techora.inventory.application.port.catalog.InventoryCatalogProduct;
import com.techora.inventory.application.result.InventoryStockSnapshot;
import com.techora.inventory.application.view.InventoryCategoryView;
import com.techora.inventory.application.view.InventoryProductStockView;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryItemMapper {

    public PageResponse<InventoryProductStockView> toPageView(
            Page<InventoryItemEntity> items,
            Map<UUID, InventoryCatalogProduct> productsById) {

        List<InventoryProductStockView> productItems = items.getContent().stream()
                .map(item -> toProductStockView(
                        productsById.get(item.getProductId()),
                        item.getQuantityOnHand()
                ))
                .toList();

        return new PageResponse<>(
                productItems,
                items.getNumber(),
                items.getSize(),
                items.getTotalElements(),
                items.getTotalPages()
        );
    }

    public InventoryProductStockView toProductStockView(@NonNull InventoryCatalogProduct product, int quantityOnHand) {
        return new InventoryProductStockView(
                product.id(),
                product.name(),
                product.sku(),
                product.slug(),
                product.description(),
                product.price(),
                quantityOnHand,
                product.status(),
                InventoryCategoryView.from(product.category()),
                product.createdAt(),
                product.updatedAt()
        );
    }

    public InventoryStockSnapshot toStockSnapshot(InventoryItemEntity item) {
        return new InventoryStockSnapshot(
                item.getProductId(),
                item.getQuantityOnHand(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                item.getUpdatedAt()
        );
    }
}
