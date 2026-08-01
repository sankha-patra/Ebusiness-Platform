package com.ebusiness.payment.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Thin Kafka payload for payment-confirmed — no shared DTO JAR. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEvent {
    private String paymentId;
    private String orderId;
    private LocalDateTime timestamp;

    public PaymentConfirmedEvent(String paymentId, String orderId) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.timestamp = LocalDateTime.now();
    }
}
