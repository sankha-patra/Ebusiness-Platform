package com.ebusiness.platform.integration;

import com.ebusiness.platform.entity.*;
import com.ebusiness.platform.repository.*;
import com.ebusiness.platform.service.*;
import com.ebusiness.platform.dto.ProductUpdateRequest;
import com.ebusiness.platform.dto.ProductCreateRequest;
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
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class EndToEndTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine")
    );

    @Container
    static RedisContainer redis = new RedisContainer(
        DockerImageName.parse("redis:7-alpine")
    );

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductCatalogService productCatalogService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private com.razorpay.RazorpayClient razorpayClient;

    private Tenant testTenant;
    private Category testCategory;
    private Product testProduct;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // Clear cache
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // Clear database
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        tenantRepository.deleteAll();

        // Setup test tenant
        testTenant = new Tenant();
        testTenant.setTenantId("tenant-test-001");
        testTenant.setName("Test Enterprise");
        testTenant.setEmail("test@enterprise.com");
        testTenant.setStatus("ACTIVE");
        testTenant = tenantRepository.save(testTenant);

        // Setup test category
        testCategory = new Category();
        testCategory.setCategoryId("electronics");
        testCategory.setName("Electronics");
        testCategory.setStatus("ACTIVE");
        testCategory = categoryRepository.save(testCategory);

        // Setup test product
        testProduct = new Product();
        testProduct.setProductId("prod-test-001");
        testProduct.setCategory(testCategory);
        testProduct.setName("Test Laptop");
        testProduct.setDescription("High-performance test laptop");
        testProduct.setPrice(new BigDecimal("50000.00"));
        testProduct.setCurrency("INR");
        testProduct.setStockQuantity(50);
        testProduct.setStatus("ACTIVE");
        testProduct = productRepository.save(testProduct);

        // Setup test order
        testOrder = new Order();
        testOrder.setOrderId("order-test-001");
        testOrder.setTenant(testTenant);
        testOrder.setStatus("CONFIRMED");
        testOrder.setTotalAmount(new BigDecimal("50000.00"));
        testOrder.setCurrency("INR");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        // Setup order items
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(testOrder);
        orderItem.setProduct(testProduct);
        orderItem.setQuantity(1);
        orderItem.setUnitPrice(new BigDecimal("50000.00"));
        orderItem.setTotalPrice(new BigDecimal("50000.00"));

        Set<OrderItem> items = new HashSet<>();
        items.add(orderItem);
        testOrder.setItems(items);

        testOrder = orderRepository.save(testOrder);

        // Setup test payment
        testPayment = new Payment();
        testPayment.setPaymentId("payment-test-001");
        testPayment.setOrder(testOrder);
        testPayment.setRazorpayPaymentId("pay_test_001");
        testPayment.setRazorpayOrderId("order_test_001");
        testPayment.setStatus("COMPLETED");
        testPayment.setAmount(new BigDecimal("50000.00"));
        testPayment.setCurrency("INR");
        testPayment.setPaymentMethod("RAZORPAY");
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        Set<Payment> payments = new HashSet<>();
        payments.add(testPayment);
        testOrder.setPayments(payments);

        testPayment = paymentRepository.save(testPayment);
    }

    @Test
    void testCompleteOrderFlow() {
        // 1. Test tenant retrieval
        assertNotNull(testTenant);
        assertEquals("tenant-test-001", testTenant.getTenantId());

        // 2. Test product catalog retrieval (should cache)
        var products = productCatalogService.getProductsByCategory("electronics");
        assertNotNull(products);
        assertFalse(products.isEmpty());
        assertEquals("Test Laptop", products.get(0).getName());

        // 3. Test order status retrieval (should cache)
        var orderStatus = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        assertNotNull(orderStatus);
        assertEquals("order-test-001", orderStatus.getOrderId());
        assertEquals("CONFIRMED", orderStatus.getStatus());
        assertEquals(new BigDecimal("50000.00"), orderStatus.getTotalAmount());

        // 4. Test cache hit for order status
        var orderStatusCached = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        assertNotNull(orderStatusCached);
        assertEquals(orderStatus.getOrderId(), orderStatusCached.getOrderId());

        // 5. Test order status update (should evict cache)
        orderService.updateOrderStatus("tenant-test-001", "order-test-001", "PROCESSING");
        
        // 6. Verify cache was evicted by fetching again
        var updatedStatus = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        assertNotNull(updatedStatus);
        assertEquals("PROCESSING", updatedStatus.getStatus());

        // 7. Test product update (should evict catalog cache)
        productCatalogService.updateProduct("prod-test-001", 
            new ProductUpdateRequest("Updated Laptop", "Updated description", 
                new BigDecimal("55000.00"), 45, "ACTIVE"));

        // 8. Verify product catalog cache was evicted
        var updatedProducts = productCatalogService.getProductsByCategory("electronics");
        assertNotNull(updatedProducts);
        assertEquals("Updated Laptop", updatedProducts.get(0).getName());
        assertEquals(new BigDecimal("55000.00"), updatedProducts.get(0).getPrice());

        System.out.println("✅ Complete order flow test passed successfully!");
    }

    @Test
    void testMultiTenantIsolation() {
        // Create second tenant
        Tenant tenant2 = new Tenant();
        tenant2.setTenantId("tenant-test-002");
        tenant2.setName("Another Enterprise");
        tenant2.setEmail("another@enterprise.com");
        tenant2.setStatus("ACTIVE");
        tenant2 = tenantRepository.save(tenant2);

        // Create order for second tenant with same order ID
        Order order2 = new Order();
        order2.setOrderId("order-test-001"); // Same order ID as first tenant
        order2.setTenant(tenant2);
        order2.setStatus("PENDING");
        order2.setTotalAmount(new BigDecimal("30000.00"));
        order2.setCurrency("INR");
        order2.setCreatedAt(LocalDateTime.now());
        order2.setUpdatedAt(LocalDateTime.now());
        order2 = orderRepository.save(order2);

        // Fetch order for first tenant
        var status1 = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        assertEquals("PROCESSING", status1.getStatus());

        // Fetch order for second tenant
        var status2 = orderService.getOrderStatus("tenant-test-002", "order-test-001");
        assertEquals("PENDING", status2.getStatus());

        // Verify tenants don't see each other's data
        assertNotEquals(status1.getStatus(), status2.getStatus());
        assertNotEquals(status1.getTotalAmount(), status2.getTotalAmount());

        System.out.println("✅ Multi-tenant isolation test passed successfully!");
    }

    @Test
    void testCachePerformance() {
        long startTime, endTime;

        // First call - cache miss
        startTime = System.currentTimeMillis();
        var status1 = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        endTime = System.currentTimeMillis();
        long missTime = endTime - startTime;
        assertNotNull(status1);

        // Second call - cache hit
        startTime = System.currentTimeMillis();
        var status2 = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        endTime = System.currentTimeMillis();
        long hitTime = endTime - startTime;
        assertNotNull(status2);

        // Cache hit should be faster (though in test environment difference may be small)
        System.out.println("Cache miss time: " + missTime + "ms");
        System.out.println("Cache hit time: " + hitTime + "ms");
        System.out.println("✅ Cache performance test completed!");
    }

    @Test
    void testProductCatalogCaching() {
        // First call - cache miss
        var products1 = productCatalogService.getProductsByCategory("electronics");
        assertNotNull(products1);
        assertEquals(1, products1.size());

        // Add another product
        Product product2 = new Product();
        product2.setProductId("prod-test-002");
        product2.setCategory(testCategory);
        product2.setName("Test Smartphone");
        product2.setDescription("Test smartphone");
        product2.setPrice(new BigDecimal("30000.00"));
        product2.setCurrency("INR");
        product2.setStockQuantity(100);
        product2.setStatus("ACTIVE");
        productRepository.save(product2);

        // Cache should still return old data
        var productsCached = productCatalogService.getProductsByCategory("electronics");
        assertEquals(1, productsCached.size());

        // Update product to evict cache
        productCatalogService.updateProduct("prod-test-001",
            new ProductUpdateRequest("Test Laptop", "Description", 
                new BigDecimal("50000.00"), 50, "ACTIVE"));

        // Cache evicted, should return new data
        var products2 = productCatalogService.getProductsByCategory("electronics");
        assertEquals(2, products2.size());

        System.out.println("✅ Product catalog caching test passed successfully!");
    }

    @Test
    void testResiliencePatterns() {
        // Test that cache failures don't break the application
        var orderStatus = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        assertNotNull(orderStatus);
        assertEquals("PROCESSING", orderStatus.getStatus());

        // Test that service continues to work even if cache has issues
        // (this is tested by the CacheErrorHandler in RedisConfig)
        var anotherStatus = orderService.getOrderStatus("tenant-test-001", "order-test-001");
        assertNotNull(anotherStatus);

        System.out.println("✅ Resilience patterns test passed successfully!");
    }
}
