package com.techora.inventory.application.service;

import com.techora.common.application.aop.BusinessException;
import com.techora.common.application.constant.ResponseCode;
import com.techora.inventory.application.repository.InventoryItemRepository;
import com.techora.inventory.domain.entity.InventoryItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryStockQueryService {
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional(readOnly = true)
    public int getQuantityOnHand(UUID productId) {
        return inventoryItemRepository.findQuantityOnHandByProductId(productId)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public int getAvailableQuantity(UUID productId) {
        return inventoryItemRepository.findAvailableQuantityByProductId(productId)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Map<UUID, Integer> getAvailableQuantities(Collection<UUID> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return inventoryItemRepository.findByProductIdIn(productIds).stream()
                .collect(Collectors.toMap(
                        InventoryItemEntity::getProductId,
                        InventoryItemEntity::availableQuantity));
    }

    @Transactional(readOnly = true)
    public void validateAvailableQuantity(UUID productId, int requestedQuantity) {
        if (requestedQuantity <= 0 || getAvailableQuantity(productId) < requestedQuantity) {
            throw new BusinessException(ResponseCode.INSUFFICIENT_STOCK);
        }
    }
}
