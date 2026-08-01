package com.ebusiness.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmedEvent {
    private String paymentId;
    private String orderId;
    private LocalDateTime timestamp;
}
