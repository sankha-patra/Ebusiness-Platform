package com.ebusiness.platform.service;

import com.ebusiness.platform.event.OrderStatusChangeEvent;
import com.ebusiness.platform.event.PaymentConfirmedEvent;
import com.ebusiness.platform.event.PaymentWebhookEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"payment-webhooks", "order-status-updates", "payment-confirmed"})
@DirtiesContext
@ActiveProfiles("test")
class KafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Autowired
    private KafkaConsumerService kafkaConsumerService;

    @BeforeEach
    void setUp() {
        // Reset any test state
    }

    @AfterEach
    void tearDown() {
        // Cleanup
    }

    @Test
    void testPublishPaymentWebhookEvent() {
        // Arrange
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setTenantId("tenant-001");
        event.setOrderId("order-001");
        event.setRazorpayPaymentId("pay_001");
        event.setRazorpayOrderId("order_001");
        event.setStatus("COMPLETED");
        event.setPaymentMethod("card");
        event.setEventType("payment.captured");

        // Act
        assertDoesNotThrow(() -> kafkaProducerService.publishPaymentWebhookEvent(event));

        // Assert - Event should be published without error
        // In a real integration test, you would consume and verify the message
    }

    @Test
    void testPublishOrderStatusChangeEvent() {
        // Arrange
        String tenantId = "tenant-001";
        String orderId = "order-001";
        String oldStatus = "PENDING";
        String newStatus = "CONFIRMED";

        // Act
        assertDoesNotThrow(() -> 
            kafkaProducerService.publishOrderStatusChangeEvent(tenantId, orderId, oldStatus, newStatus)
        );

        // Assert - Event should be published without error
    }

    @Test
    void testPublishPaymentConfirmedEvent() {
        // Arrange
        String paymentId = "payment-001";
        String orderId = "order-001";

        // Act
        assertDoesNotThrow(() -> 
            kafkaProducerService.publishPaymentConfirmedEvent(paymentId, orderId)
        );

        // Assert - Event should be published without error
    }

    @Test
    void testKafkaTemplateSendMessage() throws Exception {
        // Arrange
        String topic = "order-status-updates";
        String key = "order-001";
        OrderStatusChangeEvent event = new OrderStatusChangeEvent();
        event.setTenantId("tenant-001");
        event.setOrderId("order-001");
        event.setOldStatus("PENDING");
        event.setNewStatus("CONFIRMED");
        event.setTimestamp(LocalDateTime.now());

        // Act
        var future = kafkaTemplate.send(topic, key, event);
        future.get(5, TimeUnit.SECONDS); // Wait for send to complete

        // Assert - Message should be sent successfully
        assertTrue(future.isDone());
    }

    @Test
    void testKafkaMessageSerialization() {
        // Arrange
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setTenantId("tenant-001");
        event.setOrderId("order-001");
        event.setRazorpayPaymentId("pay_001");
        event.setRazorpayOrderId("order_001");
        event.setStatus("COMPLETED");
        event.setPaymentMethod("card");
        event.setEventType("payment.captured");
        event.setTimestamp(LocalDateTime.now());

        // Act & Assert - Should serialize without error
        assertDoesNotThrow(() -> {
            kafkaTemplate.send("payment-webhooks", event.getRazorpayPaymentId(), event);
        });
    }

    @Test
    void testMultipleMessagePublishing() throws Exception {
        // Arrange
        int messageCount = 5;

        // Act - Publish multiple messages
        for (int i = 0; i < messageCount; i++) {
            OrderStatusChangeEvent event = new OrderStatusChangeEvent();
            event.setTenantId("tenant-001");
            event.setOrderId("order-" + i);
            event.setOldStatus("PENDING");
            event.setNewStatus("CONFIRMED");
            event.setTimestamp(LocalDateTime.now());
            
            kafkaTemplate.send("order-status-updates", event.getOrderId(), event).get(5, TimeUnit.SECONDS);
        }

        // Assert - All messages should be published without exception
        // (the test would fail if any send operation threw an exception)
    }

    @Test
    void testKafkaProducerServiceNotNull() {
        // Assert
        assertNotNull(kafkaProducerService, "KafkaProducerService should be autowired");
    }

    @Test
    void testKafkaConsumerServiceNotNull() {
        // Assert
        assertNotNull(kafkaConsumerService, "KafkaConsumerService should be autowired");
    }

    @Test
    void testKafkaTemplateNotNull() {
        // Assert
        assertNotNull(kafkaTemplate, "KafkaTemplate should be autowired");
    }
}
