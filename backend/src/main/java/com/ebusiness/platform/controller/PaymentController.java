package com.ebusiness.platform.controller;

import com.ebusiness.platform.dto.PaymentStatusResponse;
import com.ebusiness.platform.dto.PaymentVerifyRequest;
import com.ebusiness.platform.service.PaymentService;
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
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(
            @PathVariable String razorpayOrderId) {
        
        log.info("GET /api/v1/payments/{}/status", razorpayOrderId);
        PaymentStatusResponse response = paymentService.getPaymentStatus(razorpayOrderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, String>> createRazorpayOrder(
            @RequestParam BigDecimal amount,
            @RequestParam String receipt) {
        
        log.info("POST /api/v1/payments/create-order - amount: {}, receipt: {}", amount, receipt);
        Map<String, String> response = paymentService.createRazorpayOrder(amount, receipt);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody PaymentVerifyRequest request) {
        log.info("POST /api/v1/payments/verify - order={}", request != null ? request.getRazorpay_order_id() : null);
        Map<String, String> result = paymentService.verifyPayment(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        
        log.info("POST /api/v1/payments/webhook - Received Razorpay webhook");
        
        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook received without X-Razorpay-Signature header");
            return ResponseEntity.badRequest().build();
        }
        
        try {
            Map<String, String> result = paymentService.processWebhook(rawBody, signature);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
