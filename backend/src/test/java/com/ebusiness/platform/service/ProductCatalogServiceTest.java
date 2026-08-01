package com.ebusiness.platform.service;

import com.ebusiness.platform.dto.ProductCreateRequest;
import com.ebusiness.platform.dto.ProductUpdateRequest;
import com.ebusiness.platform.entity.Category;
import com.ebusiness.platform.entity.Product;
import com.ebusiness.platform.repository.ProductRepository;
import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ProductCatalogServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine")
    );

    @Container
    static RedisContainer redis = new RedisContainer(
        DockerImageName.parse("redis:7-alpine")
    );

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductRepository productRepository;

    private Category testCategory;
    private Product testProduct1;
    private Product testProduct2;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Setup test data
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setCategoryId("electronics");
        testCategory.setName("Electronics");
        testCategory.setStatus("ACTIVE");

        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setProductId("prod-001");
        testProduct1.setCategory(testCategory);
        testProduct1.setName("Laptop");
        testProduct1.setDescription("High-performance laptop");
        testProduct1.setPrice(new BigDecimal("50000.00"));
        testProduct1.setCurrency("INR");
        testProduct1.setStockQuantity(50);
        testProduct1.setStatus("ACTIVE");
        testProduct1.setCreatedAt(LocalDateTime.now());
        testProduct1.setUpdatedAt(LocalDateTime.now());

        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setProductId("prod-002");
        testProduct2.setCategory(testCategory);
        testProduct2.setName("Smartphone");
        testProduct2.setDescription("Latest smartphone");
        testProduct2.setPrice(new BigDecimal("30000.00"));
        testProduct2.setCurrency("INR");
        testProduct2.setStockQuantity(100);
        testProduct2.setStatus("ACTIVE");
        testProduct2.setCreatedAt(LocalDateTime.now());
        testProduct2.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testGetProductsByCategoryCacheMiss() {
        // Arrange
        when(productRepository.findByCategoryId("electronics"))
            .thenReturn(Arrays.asList(testProduct1, testProduct2));

        // Act
        var products = productCatalogService.getProductsByCategory("electronics");

        // Assert
        assertNotNull(products);
        assertEquals(2, products.size());
        assertEquals("Laptop", products.get(0).getName());
        assertEquals("Smartphone", products.get(1).getName());
        
        // Verify repository was called (cache miss)
        verify(productRepository, times(1)).findByCategoryId("electronics");
    }

    @Test
    void testGetProductsByCategoryCacheHit() {
        // Arrange
        when(productRepository.findByCategoryId("electronics"))
            .thenReturn(Arrays.asList(testProduct1, testProduct2));

        // Act - First call (cache miss)
        var products1 = productCatalogService.getProductsByCategory("electronics");
        assertNotNull(products1);

        // Act - Second call (cache hit)
        var products2 = productCatalogService.getProductsByCategory("electronics");
        assertNotNull(products2);

        // Assert
        assertEquals(products1.size(), products2.size());
        assertEquals(products1.get(0).getName(), products2.get(0).getName());
        
        // Verify repository was called only once (second call hit cache)
        verify(productRepository, times(1)).findByCategoryId("electronics");
    }

    @Test
    void testGetAllProductsCache() {
        // Arrange
        when(productRepository.findAllActive())
            .thenReturn(Arrays.asList(testProduct1, testProduct2));

        // Act - First call
        var products1 = productCatalogService.getAllProducts();
        assertNotNull(products1);

        // Act - Second call
        var products2 = productCatalogService.getAllProducts();
        assertNotNull(products2);

        // Assert
        verify(productRepository, times(1)).findAllActive();
    }

    @Test
    void testUpdateProductEvictsCache() {
        // Arrange
        when(productRepository.findByCategoryId("electronics"))
            .thenReturn(Arrays.asList(testProduct1, testProduct2));
        when(productRepository.findByProductId("prod-001"))
            .thenReturn(java.util.Optional.of(testProduct1));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct1);

        // Act - Cache products
        productCatalogService.getProductsByCategory("electronics");
        verify(productRepository, times(1)).findByCategoryId("electronics");

        // Act - Update product (should evict cache)
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setName("Updated Laptop");
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("55000.00"));
        request.setStockQuantity(45);
        request.setStatus("ACTIVE");
        
        productCatalogService.updateProduct("prod-001", request);

        // Act - Fetch again (should be cache miss)
        productCatalogService.getProductsByCategory("electronics");

        // Assert - Repository should be called twice (initial + after eviction)
        verify(productRepository, times(2)).findByCategoryId("electronics");
    }

    @Test
    void testCreateProductEvictsCache() {
        // Arrange
        when(productRepository.findAllActive())
            .thenReturn(Arrays.asList(testProduct1));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct1);

        // Act - Cache all products
        productCatalogService.getAllProducts();
        verify(productRepository, times(1)).findAllActive();

        // Act - Create new product (should evict cache)
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductId("prod-003");
        request.setName("Tablet");
        request.setDescription("New tablet");
        request.setPrice(new BigDecimal("25000.00"));
        request.setCurrency("INR");
        request.setStockQuantity(30);
        request.setStatus("ACTIVE");
        
        productCatalogService.createProduct(request);

        // Act - Fetch again (should be cache miss)
        productCatalogService.getAllProducts();

        // Assert - Repository should be called twice (initial + after eviction)
        verify(productRepository, times(2)).findAllActive();
    }

    @Test
    void testDifferentCategoriesHaveSeparateCacheEntries() {
        // Arrange
        when(productRepository.findByCategoryId("electronics"))
            .thenReturn(Arrays.asList(testProduct1));
        when(productRepository.findByCategoryId("clothing"))
            .thenReturn(Arrays.asList(testProduct2));

        // Act - Fetch electronics
        var electronics = productCatalogService.getProductsByCategory("electronics");
        assertNotNull(electronics);

        // Act - Fetch clothing
        var clothing = productCatalogService.getProductsByCategory("clothing");
        assertNotNull(clothing);

        // Assert - Both categories should be cached separately
        verify(productRepository, times(1)).findByCategoryId("electronics");
        verify(productRepository, times(1)).findByCategoryId("clothing");
    }

    @Test
    void testAllProductsCacheIsIndependentFromCategoryCache() {
        when(productRepository.findByCategoryId("electronics"))
            .thenReturn(Arrays.asList(testProduct1));
        when(productRepository.findAllActive())
            .thenReturn(Arrays.asList(testProduct1, testProduct2));

        var categoryOnly = productCatalogService.getProductsByCategory("electronics");
        assertEquals(1, categoryOnly.size());

        var all = productCatalogService.getAllProducts();
        assertEquals(2, all.size());

        // Second all-products call must hit productsAll cache, not reuse category cache
        productCatalogService.getAllProducts();

        verify(productRepository, times(1)).findByCategoryId("electronics");
        verify(productRepository, times(1)).findAllActive();
        assertNotNull(cacheManager.getCache("productsAll"));
        assertNotNull(cacheManager.getCache("productsByCategory"));
        assertNotEquals(
            cacheManager.getCache("productsAll"),
            cacheManager.getCache("productsByCategory")
        );
    }
}
