package com.techora.catalog;

import com.techora.catalog.application.model.ProductView;
import com.techora.catalog.controller.constant.ProductPageConstant;
import com.techora.catalog.controller.request.ProductFilter;
import com.techora.catalog.domain.valueobject.ProductStatus;
import com.techora.catalog.projection.dto.CategoryProjectionSnapshot;
import com.techora.catalog.projection.dto.ProductProjectionSnapshot;
import com.techora.catalog.projection.entity.ProductReadModelEntity;
import com.techora.catalog.projection.event.ProductProjectionChangedEvent;
import com.techora.catalog.projection.handler.ProductProjectionEventHandler;
import com.techora.catalog.projection.repository.ProductReadModelRepository;
import com.techora.catalog.projection.service.ProductReadModelService;
import com.techora.common.application.dto.response.PageResponse;
import com.techora.common.infra.cache.CacheNames;
import com.techora.common.infra.config.prop.RedisCacheBypassProperties;
import com.techora.inventory.domain.event.InventoryStockChangedEvent;
import com.techora.testsupport.AbstractIntegrationTest;
import com.techora.testsupport.ConcurrentTestRunner;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.testcontainers.DockerClientFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductReadModelCacheIT extends AbstractIntegrationTest {

    private static final String CACHE_ERROR_METRIC = "techora.cache.errors";

    @Autowired
    private ProductReadModelService productReadModelService;

    @Autowired
    private ProductReadModelRepository productReadModelRepository;

    @Autowired
    private ProductProjectionEventHandler productProjectionEventHandler;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CaffeineCacheManager caffeineCacheManager;

    @Autowired
    private RedisCacheBypassProperties redisCacheBypassProperties;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private boolean redisUnavailable;

    @BeforeEach
    void setUp() {
        productReadModelRepository.deleteAll();
        clearCache(CacheNames.PRODUCT_DETAIL_BY_SLUG);
        clearCache(CacheNames.PRODUCT_LISTING);
        statistics().setStatisticsEnabled(true);
        resetDatabaseQueryCounter();
    }

    @AfterEach
    void tearDown() {
        restoreRedisIfUnavailable();
    }

    @Test
    void productDetailCacheHitAvoidsDatabaseRead() {
        String slug = "cacheable-product";
        ProductReadModelEntity readModel = productReadModelRepository.saveAndFlush(activeReadModel(slug));
        resetDatabaseQueryCounter();

        ProductView firstRead = productReadModelService.getPublicProductBySlug(slug);
        long queryCountAfterFirstRead = databaseQueryCount();

        ProductView cachedRead = productReadModelService.getPublicProductBySlug(slug);
        long queryCountAfterCachedRead = databaseQueryCount();

        assertThat(firstRead.id()).isEqualTo(readModel.getProductId());
        assertThat(cachedRead).isEqualTo(firstRead);
        assertThat(queryCountAfterFirstRead).isGreaterThan(0);
        assertThat(queryCountAfterCachedRead).isEqualTo(queryCountAfterFirstRead);
    }

    @Test
    void productDetailPreciseEvictionAfterProductUpdate() {
        String slug = "updated-cacheable-product";
        ProductReadModelEntity readModel = productReadModelRepository.saveAndFlush(activeReadModel(slug));

        ProductView cachedBeforeUpdate = productReadModelService.getPublicProductBySlug(slug);
        assertThat(cachedBeforeUpdate.name()).isEqualTo("Cacheable Product");

        productProjectionEventHandler.on(ProductProjectionChangedEvent.of(
                updatedSnapshot(readModel, "Updated Cacheable Product")
        ));
        resetDatabaseQueryCounter();

        ProductView afterUpdate = productReadModelService.getPublicProductBySlug(slug);
        long queryCountAfterEvictedRead = databaseQueryCount();

        assertThat(afterUpdate.name()).isEqualTo("Updated Cacheable Product");
        assertThat(queryCountAfterEvictedRead).isGreaterThan(0);
    }

    @Test
    void productListingCacheIsTtlOnlyAndNotEvictedByProductOrStockChanges() {
        String slug = "listing-cacheable-product";
        ProductReadModelEntity readModel = productReadModelRepository.saveAndFlush(activeReadModel(slug));
        ProductFilter filter = ProductFilter.of(null, null);
        Pageable pageable = defaultListingPageable();

        PageResponse<ProductView> cachedBeforeChanges = productReadModelService.searchPublicProducts(filter, pageable);
        assertThat(cachedBeforeChanges.items())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.name()).isEqualTo("Cacheable Product");
                    assertThat(product.stockQuantity()).isEqualTo(25);
                });

        productProjectionEventHandler.on(ProductProjectionChangedEvent.of(
                updatedSnapshot(readModel, "Updated Cacheable Product")
        ));
        productProjectionEventHandler.on(InventoryStockChangedEvent.of(
                readModel.getProductId(),
                7,
                Instant.now()
        ));
        resetDatabaseQueryCounter();

        PageResponse<ProductView> cachedAfterChanges = productReadModelService.searchPublicProducts(filter, pageable);
        long queryCountAfterCachedRead = databaseQueryCount();

        assertThat(queryCountAfterCachedRead).isZero();
        assertThat(cachedAfterChanges.items())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.name()).isEqualTo("Cacheable Product");
                    assertThat(product.stockQuantity()).isEqualTo(25);
                });
    }

    @Test
    void productDetailFallsBackToDatabaseAndWarmsLocalCacheWhenRedisUnavailable() {
        String slug = "redis-down-product";
        ProductReadModelEntity readModel = productReadModelRepository.saveAndFlush(activeReadModel(slug));

        ProductView warmed = productReadModelService.getPublicProductBySlug(slug);
        assertThat(warmed.id()).isEqualTo(readModel.getProductId());

        clearLocalCache(CacheNames.PRODUCT_DETAIL_BY_SLUG);
        makeRedisUnavailable();
        resetDatabaseQueryCounter();

        ProductView loadedFromDatabase = productReadModelService.getPublicProductBySlug(slug);
        long queryCountAfterDatabaseFallback = databaseQueryCount();

        resetDatabaseQueryCounter();
        ProductView loadedFromLocalFallback = productReadModelService.getPublicProductBySlug(slug);
        long queryCountAfterLocalFallback = databaseQueryCount();

        assertThat(loadedFromDatabase).isEqualTo(warmed);
        assertThat(queryCountAfterDatabaseFallback).isGreaterThan(0);
        assertThat(loadedFromLocalFallback).isEqualTo(warmed);
        assertThat(queryCountAfterLocalFallback).isZero();
    }

    @Test
    void productDetailSkipsRedisAfterBypassOpensFromRepeatedFailures() {
        String slug = "redis-bypass-product";
        ProductReadModelEntity readModel = productReadModelRepository.saveAndFlush(activeReadModel(slug));

        makeRedisUnavailable();
        openRedisBypassAfterRepeatedProductDetailFailures();

        clearLocalCache(CacheNames.PRODUCT_DETAIL_BY_SLUG);
        double redisGetErrorsBeforeProductRead = redisGetErrorCountForProductDetail();
        double redisPutErrorsBeforeProductRead = redisPutErrorCountForProductDetail();
        resetDatabaseQueryCounter();

        ProductView product = productReadModelService.getPublicProductBySlug(slug);
        long databaseQueryCount = databaseQueryCount();

        assertThat(product.id()).isEqualTo(readModel.getProductId());
        assertThat(databaseQueryCount).isGreaterThan(0);
        assertThat(redisGetErrorCountForProductDetail()).isEqualTo(redisGetErrorsBeforeProductRead);
        assertThat(redisPutErrorCountForProductDetail()).isEqualTo(redisPutErrorsBeforeProductRead);
    }

    @Test
    void concurrentProductDetailReadsForSameCacheMissShareOneDatabaseLoad() throws Exception {
        int concurrentReads = 20;
        String slug = "single-flight-product";
        ProductReadModelEntity readModel = productReadModelRepository.saveAndFlush(activeReadModel(slug));

        clearCache(CacheNames.PRODUCT_DETAIL_BY_SLUG);
        resetDatabaseQueryCounter();

        List<ProductView> products = ConcurrentTestRunner.run(
                concurrentReads,
                () -> productReadModelService.getPublicProductBySlug(slug)
        );
        long databaseQueryCount = databaseQueryCount();

        assertThat(products).hasSize(concurrentReads);
        assertThat(products).allSatisfy(product -> assertThat(product.id()).isEqualTo(readModel.getProductId()));
        assertThat(databaseQueryCount).isEqualTo(1);
    }

    private ProductReadModelEntity activeReadModel(String slug) {
        Instant now = Instant.now();
        UUID productId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        return ProductReadModelEntity.builder()
                .productId(productId)
                .name("Cacheable Product")
                .sku("CACHE-" + productId)
                .slug(slug)
                .description("Product used to verify detail cache")
                .price(BigDecimal.valueOf(129_00, 2))
                .stockQuantity(25)
                .status(ProductStatus.ACTIVE)
                .categoryId(categoryId)
                .categoryName("Cache Category")
                .categorySlug("cache-category")
                .categoryDescription("Category used to verify detail cache")
                .categoryActive(true)
                .categoryCreatedAt(now)
                .categoryUpdatedAt(now)
                .productCreatedAt(now)
                .productUpdatedAt(now)
                .build();
    }

    private ProductProjectionSnapshot updatedSnapshot(ProductReadModelEntity readModel, String updatedName) {
        Instant now = Instant.now();
        return new ProductProjectionSnapshot(
                readModel.getProductId(),
                updatedName,
                readModel.getSku(),
                readModel.getSlug(),
                readModel.getDescription(),
                readModel.getPrice(),
                readModel.getStockQuantity(),
                readModel.getStatus(),
                new CategoryProjectionSnapshot(
                        readModel.getCategoryId(),
                        readModel.getCategoryName(),
                        readModel.getCategorySlug(),
                        readModel.getCategoryDescription(),
                        readModel.isCategoryActive(),
                        readModel.getCategoryCreatedAt(),
                        readModel.getCategoryUpdatedAt()
                ),
                readModel.getProductCreatedAt(),
                now
        );
    }

    private Pageable defaultListingPageable() {
        return PageRequest.of(
                Integer.parseInt(ProductPageConstant.DEFAULT_PAGE),
                Integer.parseInt(ProductPageConstant.DEFAULT_SIZE),
                ProductPageConstant.CREATED_AT_DESCENDING
        );
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void clearLocalCache(String cacheName) {
        Cache cache = caffeineCacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private Cache productDetailCache() {
        Cache cache = cacheManager.getCache(CacheNames.PRODUCT_DETAIL_BY_SLUG);
        assertThat(cache).isNotNull();
        return cache;
    }

    private void openRedisBypassAfterRepeatedProductDetailFailures() {
        Cache productDetailCache = productDetailCache();
        double redisGetErrorsBeforeFailures = redisGetErrorCountForProductDetail();

        for (int failure = 0; failure < redisCacheBypassProperties.failureThreshold(); failure++) {
            productDetailCache.get("force-redis-failure-" + failure);
        }

        assertThat(redisGetErrorCountForProductDetail()).isGreaterThan(redisGetErrorsBeforeFailures);
    }

    private double redisGetErrorCountForProductDetail() {
        return redisErrorCountForProductDetail("get");
    }

    private double redisPutErrorCountForProductDetail() {
        return redisErrorCountForProductDetail("put");
    }

    private double redisErrorCountForProductDetail(String operation) {
        return meterRegistry.find(CACHE_ERROR_METRIC)
                .tag("cache", CacheNames.PRODUCT_DETAIL_BY_SLUG)
                .tag("operation", operation)
                .counters()
                .stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }

    private void makeRedisUnavailable() {
        DockerClientFactory.instance().client()
                .pauseContainerCmd(REDIS.getContainerId())
                .exec();
        redisUnavailable = true;
    }

    private void restoreRedisIfUnavailable() {
        if (!redisUnavailable) {
            return;
        }

        DockerClientFactory.instance().client()
                .unpauseContainerCmd(REDIS.getContainerId())
                .exec();
        redisUnavailable = false;
    }

    private void resetDatabaseQueryCounter() {
        statistics().clear();
    }

    private long databaseQueryCount() {
        return statistics().getPrepareStatementCount();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
