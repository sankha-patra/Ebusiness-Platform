package com.ebusiness.platform.service;

import com.ebusiness.platform.entity.Order;
import com.ebusiness.platform.repository.OrderRepository;
import com.ebusiness.platform.repository.ProcessedWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reconciliation job from Phase 3 notes.
 * Safety net for cases where Kafka message was lost or notification service failed.
 * Runs every 5 minutes, finds PAID orders that might not have been notified,
 * and republishes them to Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReconciliationService {

    private final OrderRepository orderRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final KafkaProducerService kafkaProducerService;

    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void reconcileOrders() {
        log.debug("Running order reconciliation job...");
        
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        
        // Find orders that are PAID but were created more than 5 minutes ago
        // These might have missed their Kafka notification
        List<Order> stuckOrders = orderRepository.findByStatusAndCreatedAtBefore("PAID", fiveMinutesAgo);
        
        if (stuckOrders.isEmpty()) {
            log.debug("No stuck orders found. Reconciliation complete.");
            return;
        }
        
        log.info("Found {} potentially stuck PAID orders. Checking notifications...", stuckOrders.size());
        
        for (Order order : stuckOrders) {
            try {
                // Check if notification was already sent (idempotency table)
                // We use the razorpayPaymentId as the key since that's what the webhook handler uses
                String paymentId = order.getRazorpayPaymentId();
                
                if (paymentId != null && processedWebhookRepository.existsByPaymentId(paymentId)) {
                    // Already processed via webhook, notification should have been sent
                    continue;
                }
                
                // Republish to Kafka (or directly notify)
                log.warn("Order {} (paymentId={}) appears stuck. Republishing to Kafka...", 
                    order.getOrderId(), paymentId);
                
                kafkaProducerService.publishPaymentConfirmedEvent(
                    paymentId != null ? paymentId : "reconciled-" + order.getOrderId(),
                    order.getOrderId()
                );
                
            } catch (Exception e) {
                log.error("Error reconciling order {}: {}", order.getOrderId(), e.getMessage());
            }
        }
        
        log.info("Order reconciliation job completed.");
    }
}
