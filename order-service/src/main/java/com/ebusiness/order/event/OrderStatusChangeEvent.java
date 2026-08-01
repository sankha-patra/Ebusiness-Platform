package com.ebusiness.order.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
