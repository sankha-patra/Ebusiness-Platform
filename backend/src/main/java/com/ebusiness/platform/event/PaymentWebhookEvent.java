package com.ebusiness.platform.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {
    
    private String eventId;
    private String tenantId;
    private String orderId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String status;
    private String paymentMethod;
    private LocalDateTime timestamp;
    private String eventType; // payment.captured, payment.failed, etc.
}
