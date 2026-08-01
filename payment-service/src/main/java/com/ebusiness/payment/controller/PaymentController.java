package com.ebusiness.payment.controller;

import com.ebusiness.payment.dto.PaymentStatusResponse;
import com.ebusiness.payment.dto.PaymentVerifyRequest;
import com.ebusiness.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/{razorpayOrderId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String razorpayOrderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(razorpayOrderId));
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, String>> createRazorpayOrder(
            @RequestParam BigDecimal amount,
            @RequestParam String receipt) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(amount, receipt));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (signature == null || signature.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(paymentService.processWebhook(rawBody, signature));
        } catch (RuntimeException e) {
            log.error("Webhook failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
