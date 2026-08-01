package com.ebusiness.platform.service;

import com.ebusiness.platform.event.OrderStatusChangeEvent;
import com.ebusiness.platform.event.PaymentConfirmedEvent;
import com.ebusiness.platform.event.PaymentWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class KafkaProducerService {

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    public void publishPaymentWebhookEvent(PaymentWebhookEvent event) {
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(LocalDateTime.now());
        
        if (kafkaEnabled && kafkaTemplate != null) {
            log.info("Publishing payment webhook event: {}", event);
            try {
                kafkaTemplate.send("payment-webhooks", event.getRazorpayPaymentId(), event);
                log.info("Payment webhook event published successfully: {}", event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish payment webhook event: {}", event.getEventId(), e);
            }
        } else {
            log.info("Kafka not available - would publish payment webhook event: {}", event);
            log.info("Event details: tenantId={}, orderId={}, status={}", 
                event.getTenantId(), event.getOrderId(), event.getStatus());
        }
    }

    public void publishOrderStatusChangeEvent(String tenantId, String orderId, String oldStatus, String newStatus) {
        OrderStatusChangeEvent event = new OrderStatusChangeEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setTenantId(tenantId);
        event.setOrderId(orderId);
        event.setOldStatus(oldStatus);
        event.setNewStatus(newStatus);
        event.setTimestamp(LocalDateTime.now());
        
        if (kafkaEnabled && kafkaTemplate != null) {
            log.info("Publishing order status change event: {}", event);
            try {
                kafkaTemplate.send("order-status-updates", orderId, event);
                log.info("Order status change event published successfully: {}", event.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish order status change event: {}", event.getEventId(), e);
            }
        } else {
            log.info("Kafka not available - would publish order status change event");
            log.info("Event details: tenantId={}, orderId={}, oldStatus={}, newStatus={}", 
                tenantId, orderId, oldStatus, newStatus);
        }
    }

    public void publishPaymentConfirmedEvent(String paymentId, String orderId) {
        if (kafkaEnabled && kafkaTemplate != null) {
            log.info("Publishing payment confirmed event for payment: {}, order: {}", paymentId, orderId);
            PaymentConfirmedEvent event = new PaymentConfirmedEvent(paymentId, orderId);
            try {
                kafkaTemplate.send("payment-confirmed", paymentId, event);
                log.info("Payment confirmed event published successfully for payment: {}", paymentId);
            } catch (Exception e) {
                log.error("Failed to publish payment confirmed event for payment: {}", paymentId, e);
            }
        } else {
            log.info("Kafka not available - would publish payment confirmed event");
            log.info("Event details: paymentId={}, orderId={}", paymentId, orderId);
        }
    }
}
