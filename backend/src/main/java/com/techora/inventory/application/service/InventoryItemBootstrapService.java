package com.techora.inventory.application.service;

import com.techora.inventory.application.port.catalog.InventoryCatalogPort;
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

    private final InventoryCatalogPort inventoryCatalogPort;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Set<UUID> existingProductIds = inventoryItemRepository.findExistingProductIds();

        inventoryCatalogPort.getAllProducts().stream()
                .filter(product -> !existingProductIds.contains(product.id()))
                .map(product -> toInitialItem(product.id()))
                .forEach(inventoryItemRepository::save);
    }

    private InventoryItemEntity toInitialItem(UUID productId) {
        Instant now = Instant.now();
        return InventoryItemEntity.builder()
                .productId(productId)
                .quantityOnHand(0)
                .reservedQuantity(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
