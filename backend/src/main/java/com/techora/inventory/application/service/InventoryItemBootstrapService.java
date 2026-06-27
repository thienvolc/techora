package com.techora.inventory.application.service;

import com.techora.catalog.dto.CatalogProductSnapshot;
import com.techora.catalog.service.ProductCatalogQueryService;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@Order(1)
@RequiredArgsConstructor
public class InventoryItemBootstrapService implements ApplicationRunner {

    private final ProductCatalogQueryService productCatalogQueryService;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<UUID> existingProductIds = inventoryItemRepository.findExistingProductIds();

        productCatalogQueryService.getAllProducts().stream()
                .filter(product -> !existingProductIds.contains(product.id()))
                .map(this::toInitialItem)
                .forEach(inventoryItemRepository::save);
    }

    private InventoryItemEntity toInitialItem(CatalogProductSnapshot product) {
        Instant now = Instant.now();
        return InventoryItemEntity.builder()
                .productId(product.id())
                .quantityOnHand(0)
                .reservedQuantity(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
