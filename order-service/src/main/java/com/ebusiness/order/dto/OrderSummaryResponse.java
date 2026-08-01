package com.ebusiness.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private String orderId;
    private String tenantId;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentStatus;
    private String razorpayOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
