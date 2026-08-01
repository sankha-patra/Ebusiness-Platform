package com.ebusiness.order.service;

import com.ebusiness.order.event.PaymentConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfirmedConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-confirmed", groupId = "order-service")
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("payment-confirmed received paymentId={} orderId={}",
            event.getPaymentId(), event.getOrderId());
        if (event.getOrderId() != null) {
            orderService.markPaidFromEvent(event.getOrderId());
        }
    }
}
