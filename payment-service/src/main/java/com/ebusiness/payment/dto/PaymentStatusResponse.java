package com.ebusiness.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatusResponse {
    private String paymentId;
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
