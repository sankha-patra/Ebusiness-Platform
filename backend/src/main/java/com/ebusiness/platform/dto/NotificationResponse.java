package com.ebusiness.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String notificationId;
    private String tenantId;
    private String orderId;
    private String paymentId;
    private String channel;
    private String recipient;
    private String message;
    private String status;
    private boolean read;
    private LocalDateTime createdAt;
}
