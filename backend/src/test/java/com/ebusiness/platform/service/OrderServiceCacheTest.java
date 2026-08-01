package com.ebusiness.platform.service;

import com.ebusiness.platform.entity.Order;
import com.ebusiness.platform.entity.OrderItem;
import com.ebusiness.platform.entity.Payment;
import com.ebusiness.platform.entity.Tenant;
import com.ebusiness.platform.repository.OrderRepository;
import com.ebusiness.platform.repository.TenantRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OrderServiceCacheTest {

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
    private CacheManager cacheManager;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private TenantRepository tenantRepository;

    private Tenant testTenant;
    private Order testOrder;

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
        testTenant = new Tenant();
        testTenant.setId(1L);
        testTenant.setTenantId("tenant-001");
        testTenant.setName("Test Tenant");
        testTenant.setEmail("test@tenant.com");
        testTenant.setStatus("ACTIVE");

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderId("order-001");
        testOrder.setTenant(testTenant);
        testOrder.setStatus("CONFIRMED");
        testOrder.setTotalAmount(new BigDecimal("1000.00"));
        testOrder.setCurrency("INR");
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        Set<OrderItem> items = new HashSet<>();
        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(testOrder);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("500.00"));
        item.setTotalPrice(new BigDecimal("1000.00"));
        items.add(item);
        testOrder.setItems(items);

        Set<Payment> payments = new HashSet<>();
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setPaymentId("payment-001");
        payment.setOrder(testOrder);
        payment.setStatus("COMPLETED");
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setCurrency("INR");
        payment.setPaymentMethod("RAZORPAY");
        payments.add(payment);
        testOrder.setPayments(payments);
    }

    @Test
    void testOrderStatusCacheMiss() {
        // Arrange
        when(orderRepository.findByTenantAndId("tenant-001", "order-001"))
            .thenReturn(Optional.of(testOrder));

        // Act
        var response = orderService.getOrderStatus("tenant-001", "order-001");

        // Assert
        assertNotNull(response);
        assertEquals("order-001", response.getOrderId());
        assertEquals("tenant-001", response.getTenantId());
        assertEquals("CONFIRMED", response.getStatus());
        
        // Verify repository was called (cache miss)
        verify(orderRepository, times(1)).findByTenantAndId("tenant-001", "order-001");
    }

    @Test
    void testOrderStatusCacheHit() {
        // Arrange
        when(orderRepository.findByTenantAndId("tenant-001", "order-001"))
            .thenReturn(Optional.of(testOrder));

        // Act - First call (cache miss)
        var response1 = orderService.getOrderStatus("tenant-001", "order-001");
        assertNotNull(response1);

        // Act - Second call (cache hit)
        var response2 = orderService.getOrderStatus("tenant-001", "order-001");
        assertNotNull(response2);

        // Assert
        assertEquals(response1.getOrderId(), response2.getOrderId());
        assertEquals(response1.getStatus(), response2.getStatus());
        
        // Verify repository was called only once (second call hit cache)
        verify(orderRepository, times(1)).findByTenantAndId("tenant-001", "order-001");
    }

    @Test
    void testOrderStatusCacheEvict() {
        // Arrange
        when(orderRepository.findByTenantAndId("tenant-001", "order-001"))
            .thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act - Cache the order
        orderService.getOrderStatus("tenant-001", "order-001");
        verify(orderRepository, times(1)).findByTenantAndId("tenant-001", "order-001");

        // Act - Update order status (should evict cache)
        orderService.updateOrderStatus("tenant-001", "order-001", "PROCESSING");

        // Act - Fetch again (should be cache miss)
        orderService.getOrderStatus("tenant-001", "order-001");

        // Assert - Repository should be called twice (initial + after eviction)
        verify(orderRepository, times(2)).findByTenantAndId("tenant-001", "order-001");
    }

    @Test
    void testOrderStatusNullNotCached() {
        // Arrange
        when(orderRepository.findByTenantAndId("tenant-001", "order-999"))
            .thenReturn(Optional.empty());

        // Act
        try {
            orderService.getOrderStatus("tenant-001", "order-999");
            fail("Should throw exception");
        } catch (Exception e) {
            // Expected
        }

        // Act - Try again
        try {
            orderService.getOrderStatus("tenant-001", "order-999");
            fail("Should throw exception");
        } catch (Exception e) {
            // Expected
        }

        // Assert - Repository should be called twice (null not cached)
        verify(orderRepository, times(2)).findByTenantAndId("tenant-001", "order-999");
    }

    @Test
    void testMultiTenantCacheIsolation() {
        // Arrange
        when(orderRepository.findByTenantAndId("tenant-001", "order-001"))
            .thenReturn(Optional.of(testOrder));
        when(orderRepository.findByTenantAndId("tenant-002", "order-001"))
            .thenReturn(Optional.empty());

        // Act - Fetch for tenant-001
        var response1 = orderService.getOrderStatus("tenant-001", "order-001");
        assertNotNull(response1);

        // Act - Try to fetch same order ID for different tenant
        try {
            orderService.getOrderStatus("tenant-002", "order-001");
            fail("Should throw exception");
        } catch (Exception e) {
            // Expected - different tenant should not get cached data
        }

        // Assert - Verify different tenants have separate cache entries
        verify(orderRepository, times(1)).findByTenantAndId("tenant-001", "order-001");
        verify(orderRepository, times(1)).findByTenantAndId("tenant-002", "order-001");
    }

    @Test
    void testConcurrentRequests() throws InterruptedException {
        // Arrange
        when(orderRepository.findByTenantAndId("tenant-001", "order-001"))
            .thenReturn(Optional.of(testOrder));

        // Act - Simulate concurrent requests
        Thread[] threads = new Thread[5];
        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                orderService.getOrderStatus("tenant-001", "order-001");
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Assert - Despite 5 concurrent requests, repository should be called minimally
        // (exact count depends on Spring Cache implementation)
        verify(orderRepository, atMost(2)).findByTenantAndId("tenant-001", "order-001");
    }
}
