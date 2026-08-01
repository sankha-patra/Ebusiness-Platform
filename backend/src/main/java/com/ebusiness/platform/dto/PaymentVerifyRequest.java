package com.ebusiness.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerifyRequest {
    private String razorpay_order_id;
    private String razorpay_payment_id;
    private String razorpay_signature;
}
