package com.ebusiness.payment.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Thin Kafka payload for order-status-updates. */
@Data
@NoArgsConstructor
public class OrderStatusChangeEvent {
    private String eventId;
    private String tenantId;
    private String orderId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime timestamp;
}
