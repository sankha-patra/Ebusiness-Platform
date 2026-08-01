package com.ebusiness.platform.controller;

import com.ebusiness.platform.event.OrderStatusChangeEvent;
import com.ebusiness.platform.event.PaymentConfirmedEvent;
import com.ebusiness.platform.event.PaymentWebhookEvent;
import com.ebusiness.platform.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/v1/load-test")
@RequiredArgsConstructor
public class LoadTestController {

    private final KafkaProducerService kafkaProducerService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @PostMapping("/kafka/payment-webhooks")
    public ResponseEntity<String> testKafkaPaymentWebhooks(
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("Starting Kafka load test for payment webhooks with {} messages", count);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    PaymentWebhookEvent event = new PaymentWebhookEvent();
                    event.setTenantId("tenant-" + (index % 3));
                    event.setOrderId("order-" + index);
                    event.setRazorpayPaymentId("pay_" + index);
                    event.setRazorpayOrderId("order_" + index);
                    event.setStatus("COMPLETED");
                    event.setPaymentMethod("card");
                    event.setEventType("payment.captured");
                    
                    kafkaProducerService.publishPaymentWebhookEvent(event);
                    Thread.sleep(100); // Small delay between messages
                } catch (Exception e) {
                    log.error("Error publishing webhook event: {}", e.getMessage());
                }
            }, executorService);
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return ResponseEntity.ok("Successfully published " + count + " payment webhook events");
    }

    @PostMapping("/kafka/order-status")
    public ResponseEntity<String> testKafkaOrderStatus(
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("Starting Kafka load test for order status updates with {} messages", count);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    kafkaProducerService.publishOrderStatusChangeEvent(
                        "tenant-" + (index % 3),
                        "order-" + index,
                        "PENDING",
                        "CONFIRMED"
                    );
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("Error publishing order status event: {}", e.getMessage());
                }
            }, executorService);
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return ResponseEntity.ok("Successfully published " + count + " order status events");
    }

    @PostMapping("/kafka/payment-confirmed")
    public ResponseEntity<String> testKafkaPaymentConfirmed(
            @RequestParam(defaultValue = "10") int count) {
        
        log.info("Starting Kafka load test for payment confirmed events with {} messages", count);
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            final int index = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    kafkaProducerService.publishPaymentConfirmedEvent(
                        "pay-loadtest-" + index,
                        "ord-loadtest-" + index
                    );
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("Error publishing payment confirmed event: {}", e.getMessage());
                }
            }, executorService);
            
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return ResponseEntity.ok("Successfully published " + count + " payment confirmed events");
    }

    @PostMapping("/mixed-traffic")
    public ResponseEntity<String> testMixedTraffic(
            @RequestParam(defaultValue = "5") int perType) {
        
        log.info("Starting mixed traffic test with {} messages per type", perType);
        
        // Test payment webhooks
        testKafkaPaymentWebhooks(perType);
        
        // Test order status updates
        testKafkaOrderStatus(perType);
        
        // Test payment confirmed events
        testKafkaPaymentConfirmed(perType);
        
        return ResponseEntity.ok("Successfully completed mixed traffic test: " + 
            (perType * 3) + " total messages");
    }

    @GetMapping("/kafka-stats")
    public ResponseEntity<String> getKafkaStats() {
        return ResponseEntity.ok(
            "Kafka Load Test Stats:\n" +
            "- Use POST /api/v1/load-test/kafka/payment-webhooks?count=N to test payment webhooks\n" +
            "- Use POST /api/v1/load-test/kafka/order-status?count=N to test order status\n" +
            "- Use POST /api/v1/load-test/kafka/payment-confirmed?count=N to test payment confirmed\n" +
            "- Use POST /api/v1/load-test/mixed-traffic?perType=N to test all event types\n" +
            "- Monitor results in Kafka UI at http://localhost:8080"
        );
    }
}
