package com.ebusiness.platform.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChangeEvent {
    
    private String eventId;
    private String tenantId;
    private String orderId;
    private String oldStatus;
    private String newStatus;
    private LocalDateTime timestamp;
}
