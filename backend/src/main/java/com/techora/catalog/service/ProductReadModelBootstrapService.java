package com.techora.catalog.service;

import com.techora.catalog.entity.ProductEntity;
import com.techora.catalog.mapper.ProductMapper;
import com.techora.catalog.repository.ProductRepository;
import com.techora.inventory.application.service.InventoryStockQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Order(2)
@RequiredArgsConstructor
public class ProductReadModelBootstrapService implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final InventoryStockQueryService inventoryStockQueryService;
    private final ProductReadModelService productReadModelService;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var products = productRepository.findAllBy();
        Map<UUID, Integer> stockByProductId = inventoryStockQueryService.getAvailableQuantities(
                products.stream()
                        .map(ProductEntity::getId)
                        .toList());

        productReadModelService.upsertAll(products.stream()
                .map(product -> productMapper.toProjectionSnapshot(
                        product,
                        stockByProductId.getOrDefault(product.getId(), 0)))
                .toList());
    }
}
