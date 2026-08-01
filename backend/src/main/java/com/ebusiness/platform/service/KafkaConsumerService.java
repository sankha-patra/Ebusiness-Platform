package com.ebusiness.platform.service;

import com.ebusiness.platform.event.OrderStatusChangeEvent;
import com.ebusiness.platform.event.PaymentConfirmedEvent;
import com.ebusiness.platform.event.PaymentWebhookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @KafkaListener(
        topics = "payment-webhooks",
        groupId = "ebusiness-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentWebhook(
            @Payload PaymentWebhookEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        log.info("Received payment webhook event: {}", event);
        
        try {
            if (event.getTenantId() != null && event.getOrderId() != null) {
                orderService.handlePaymentWebhook(event.getTenantId(), event.getOrderId());
            }

            if ("FAILED".equals(event.getStatus())) {
                log.info("Payment FAILED event for order {} — UI/orders should show PAYMENT_FAILED", event.getOrderId());
            } else if ("COMPLETED".equals(event.getStatus()) && event.getRazorpayOrderId() != null) {
                paymentService.onPaymentConfirmed(event.getRazorpayOrderId());
            }
            
            acknowledgment.acknowledge();
            log.info("Successfully processed payment webhook event");
            
        } catch (Exception e) {
            log.error("Error processing payment webhook event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
        topics = "order-status-updates",
        groupId = "ebusiness-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderStatusChange(
            @Payload OrderStatusChangeEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        log.info("Received order status change event: {} -> {}", event.getOldStatus(), event.getNewStatus());
        
        try {
            if (event.getTenantId() != null && event.getOrderId() != null) {
                orderService.handlePaymentWebhook(event.getTenantId(), event.getOrderId());
            }
            acknowledgment.acknowledge();
            log.info("Successfully processed order status change event");
            
        } catch (Exception e) {
            log.error("Error processing order status change event: {}", e.getMessage(), e);
        }
    }

    /**
     * Notebook Step 10: Notification service consumes payment/order confirmation
     * and "sends" SMS to the user. Without AWS we persist + mock SMS log;
     * the Angular app polls /notifications to show the message.
     */
    @KafkaListener(
        topics = "payment-confirmed",
        groupId = "ebusiness-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentConfirmed(
            @Payload PaymentConfirmedEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        log.info("Received payment confirmed event: paymentId={}, orderId={}",
            event.getPaymentId(), event.getOrderId());
        
        try {
            if (event.getOrderId() != null) {
                orderService.markPaidFromEvent(event.getOrderId());
            }

            notificationService.notifyPaymentSuccess(
                event.getOrderId(),
                event.getPaymentId(),
                "default"
            );

            acknowledgment.acknowledge();
            log.info("Successfully processed payment confirmed event + user notification");
            
        } catch (Exception e) {
            log.error("Error processing payment confirmed event: {}", e.getMessage(), e);
        }
    }
}
