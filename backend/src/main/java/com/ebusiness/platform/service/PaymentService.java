package com.ebusiness.platform.service;

import com.ebusiness.platform.dto.PaymentStatusResponse;
import com.ebusiness.platform.dto.PaymentVerifyRequest;
import com.ebusiness.platform.entity.Order;
import com.ebusiness.platform.entity.Payment;
import com.ebusiness.platform.entity.ProcessedWebhook;
import com.ebusiness.platform.entity.Tenant;
import com.ebusiness.platform.repository.OrderRepository;
import com.ebusiness.platform.repository.PaymentRepository;
import com.ebusiness.platform.repository.ProcessedWebhookRepository;
import com.ebusiness.platform.repository.TenantRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    public static final String DEFAULT_TENANT_ID = "default";

    private final PaymentRepository paymentRepository;
    private final ProcessedWebhookRepository processedWebhookRepository;
    private final OrderRepository orderRepository;
    private final TenantRepository tenantRepository;
    private final RazorpayClient razorpayClient;
    private final KafkaProducerService kafkaProducerService;

    @Value("${razorpay.currency}")
    private String currency;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @Cacheable(
        value = "paymentStatus",
        key = "'payment:' + #razorpayOrderId",
        unless = "#result == null || #result.status == 'PENDING'"
    )
    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String razorpayOrderId) {
        log.info("Fetching payment status for Razorpay order: {}", razorpayOrderId);

        var payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Payment not found: " + razorpayOrderId));

        return mapToPaymentStatusResponse(payment);
    }

    @CircuitBreaker(name = "razorpayApi", fallbackMethod = "createRazorpayOrderFallback")
    @Transactional
    public Map<String, String> createRazorpayOrder(BigDecimal amount, String receipt) {
        log.info("Creating Razorpay order for amount: {}, receipt: {}", amount, receipt);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(new BigDecimal("100")).intValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Tenant tenant = ensureDefaultTenant();
            String localOrderId = "ord-" + UUID.randomUUID().toString().substring(0, 8);
            String localPaymentId = "pay-" + UUID.randomUUID().toString().substring(0, 8);

            Order order = new Order();
            order.setOrderId(localOrderId);
            order.setTenant(tenant);
            order.setStatus("PAYMENT_PENDING");
            order.setTotalAmount(amount);
            order.setCurrency(currency);
            order.setRazorpayOrderId(razorpayOrderId);
            order.setNotes("receipt=" + receipt);
            if (receipt != null && receipt.startsWith("buy-")) {
                order.setNotes("receipt=" + receipt);
            }
            order.setItems(new HashSet<>());
            order.setPayments(new HashSet<>());
            order = orderRepository.save(order);

            Payment payment = new Payment();
            payment.setPaymentId(localPaymentId);
            payment.setOrder(order);
            payment.setStatus("PENDING");
            payment.setAmount(amount);
            payment.setCurrency(currency);
            payment.setPaymentMethod("RAZORPAY");
            payment.setRazorpayOrderId(razorpayOrderId);
            paymentRepository.save(payment);

            Map<String, String> response = new HashMap<>();
            response.put("razorpayOrderId", razorpayOrderId);
            response.put("orderId", localOrderId);
            response.put("paymentId", localPaymentId);
            response.put("currency", currency);
            response.put("amount", amount.multiply(new BigDecimal("100")).toPlainString());
            response.put("receipt", receipt);

            log.info("Razorpay order created and persisted: rzp={}, order={}, payment={}",
                razorpayOrderId, localOrderId, localPaymentId);
            return response;

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Failed to create Razorpay order", e);
        }
    }

    @CacheEvict(value = "paymentStatus", key = "'payment:' + #request.razorpay_order_id")
    @Transactional
    public Map<String, String> verifyPayment(PaymentVerifyRequest request) {
        if (request == null
            || isBlank(request.getRazorpay_order_id())
            || isBlank(request.getRazorpay_payment_id())
            || isBlank(request.getRazorpay_signature())) {
            throw new RuntimeException("Missing Razorpay verification fields");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpay_order_id());
            options.put("razorpay_payment_id", request.getRazorpay_payment_id());
            options.put("razorpay_signature", request.getRazorpay_signature());
            Utils.verifyPaymentSignature(options, razorpayKeySecret);
        } catch (RazorpayException e) {
            log.error("Payment signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid payment signature", e);
        }

        Payment payment = resolvePayment(request.getRazorpay_payment_id(), request.getRazorpay_order_id())
            .orElseThrow(() -> new RuntimeException("Payment not found for order: " + request.getRazorpay_order_id()));

        applyCaptured(payment, request.getRazorpay_payment_id(), request.getRazorpay_signature(), "client_verify");

        Map<String, String> result = new HashMap<>();
        result.put("status", "COMPLETED");
        result.put("outcome", "SUCCESS");
        result.put("paymentId", payment.getPaymentId());
        result.put("orderId", payment.getOrder() != null ? payment.getOrder().getOrderId() : "");
        result.put("razorpayPaymentId", request.getRazorpay_payment_id());
        result.put("razorpayOrderId", request.getRazorpay_order_id());
        result.put("message", "Payment confirmed");
        return result;
    }

    @Transactional
    public Map<String, String> processWebhook(String rawBody, String signature) {
        log.info("Processing Razorpay webhook");

        try {
            Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
            log.info("Webhook signature verified successfully");
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature", e);
        }

        JSONObject webhookPayload = new JSONObject(rawBody);
        String eventType = webhookPayload.getString("event");
        JSONObject paymentEntity = webhookPayload
            .getJSONObject("payload")
            .getJSONObject("payment")
            .getJSONObject("entity");

        String razorpayPaymentId = paymentEntity.getString("id");
        String razorpayOrderId = paymentEntity.optString("order_id", "");
        String failureReason = paymentEntity.optString("error_description",
            paymentEntity.optString("error_reason", "payment_failed"));

        log.info("Webhook event: {}, paymentId: {}, orderId: {}", eventType, razorpayPaymentId, razorpayOrderId);

        if (processedWebhookRepository.existsByPaymentId(razorpayPaymentId + ":" + eventType)
            || processedWebhookRepository.existsByPaymentId(razorpayPaymentId)) {
            log.info("Duplicate webhook detected for paymentId: {}. Already processed.", razorpayPaymentId);
            Map<String, String> result = new HashMap<>();
            result.put("status", "already_processed");
            result.put("outcome", "DUPLICATE");
            return result;
        }

        if ("payment.captured".equals(eventType) || "payment.authorized".equals(eventType)) {
            Payment payment = resolvePayment(razorpayPaymentId, razorpayOrderId)
                .orElse(null);
            if (payment != null) {
                applyCaptured(payment, razorpayPaymentId, null, eventType);
            } else {
                log.warn("No local payment for captured webhook order={}", razorpayOrderId);
            }
            saveIdempotencyRecord(razorpayPaymentId, razorpayOrderId, eventType);

            Map<String, String> result = new HashMap<>();
            result.put("status", "processed");
            result.put("outcome", "SUCCESS");
            result.put("paymentId", razorpayPaymentId);
            return result;
        }

        if ("payment.failed".equals(eventType)) {
            processPaymentFailed(razorpayPaymentId, razorpayOrderId, failureReason);
            saveIdempotencyRecord(razorpayPaymentId, razorpayOrderId, eventType);

            Map<String, String> result = new HashMap<>();
            result.put("status", "processed");
            result.put("outcome", "FAILED");
            result.put("paymentId", razorpayPaymentId);
            result.put("message", failureReason);
            return result;
        }

        Map<String, String> result = new HashMap<>();
        result.put("status", "ignored");
        result.put("outcome", "IGNORED");
        result.put("event", eventType);
        return result;
    }

    private void applyCaptured(Payment payment, String razorpayPaymentId, String signature, String source) {
        if ("COMPLETED".equals(payment.getStatus())) {
            log.info("Payment already COMPLETED: {}", payment.getPaymentId());
            return;
        }

        payment.setStatus("COMPLETED");
        payment.setRazorpayPaymentId(razorpayPaymentId);
        if (signature != null) {
            payment.setRazorpaySignature(signature);
        }
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order != null) {
            order.setStatus("PAID");
            order.setRazorpayPaymentId(razorpayPaymentId);
            orderRepository.save(order);

            String tenantId = order.getTenant() != null ? order.getTenant().getTenantId() : DEFAULT_TENANT_ID;
            kafkaProducerService.publishOrderStatusChangeEvent(
                tenantId, order.getOrderId(), "PAYMENT_PENDING", "PAID");
        }

        kafkaProducerService.publishPaymentConfirmedEvent(
            payment.getPaymentId(),
            order != null ? order.getOrderId() : payment.getRazorpayOrderId()
        );

        log.info("Payment captured via {}: localPayment={}, razorpayPayment={}",
            source, payment.getPaymentId(), razorpayPaymentId);
    }

    private void processPaymentFailed(String razorpayPaymentId, String razorpayOrderId, String failureReason) {
        log.info("Processing payment failed: {}", razorpayPaymentId);

        Optional<Payment> paymentOpt = resolvePayment(razorpayPaymentId, razorpayOrderId);
        if (paymentOpt.isEmpty()) {
            log.warn("No local payment for failed webhook order={}", razorpayOrderId);
            return;
        }

        Payment payment = paymentOpt.get();
        payment.setStatus("FAILED");
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setFailureReason(failureReason);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        if (order != null) {
            String oldStatus = order.getStatus();
            order.setStatus("PAYMENT_FAILED");
            orderRepository.save(order);

            String tenantId = order.getTenant() != null ? order.getTenant().getTenantId() : DEFAULT_TENANT_ID;
            kafkaProducerService.publishOrderStatusChangeEvent(
                tenantId, order.getOrderId(), oldStatus, "PAYMENT_FAILED");
            kafkaProducerService.publishPaymentWebhookEvent(
                buildFailedWebhookEvent(tenantId, order.getOrderId(), razorpayPaymentId, razorpayOrderId, failureReason)
            );
        }
    }

    private com.ebusiness.platform.event.PaymentWebhookEvent buildFailedWebhookEvent(
            String tenantId, String orderId, String razorpayPaymentId, String razorpayOrderId, String reason) {
        com.ebusiness.platform.event.PaymentWebhookEvent event =
            new com.ebusiness.platform.event.PaymentWebhookEvent();
        event.setTenantId(tenantId);
        event.setOrderId(orderId);
        event.setRazorpayPaymentId(razorpayPaymentId);
        event.setRazorpayOrderId(razorpayOrderId);
        event.setStatus("FAILED");
        event.setEventType("payment.failed");
        event.setPaymentMethod(reason);
        return event;
    }

    private Optional<Payment> resolvePayment(String razorpayPaymentId, String razorpayOrderId) {
        if (!isBlank(razorpayPaymentId)) {
            Optional<Payment> byPayId = paymentRepository.findByRazorpayPaymentId(razorpayPaymentId);
            if (byPayId.isPresent()) {
                return byPayId;
            }
        }
        if (!isBlank(razorpayOrderId)) {
            return paymentRepository.findByRazorpayOrderId(razorpayOrderId).stream().findFirst();
        }
        return Optional.empty();
    }

    public void saveIdempotencyRecord(String paymentId, String orderId, String eventType) {
        try {
            ProcessedWebhook record = new ProcessedWebhook();
            record.setPaymentId(paymentId);
            record.setOrderId(orderId);
            record.setEventType(eventType);
            processedWebhookRepository.save(record);
            log.info("Idempotency record saved for paymentId: {}", paymentId);
        } catch (Exception e) {
            log.error("Failed to save idempotency record for paymentId: {}", paymentId, e);
        }
    }

    @CacheEvict(value = "paymentStatus", key = "'payment:' + #razorpayOrderId")
    @Transactional
    public void onPaymentConfirmed(String razorpayOrderId) {
        log.info("Payment confirmed cache/update for Razorpay order: {}", razorpayOrderId);

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Payment not found: " + razorpayOrderId));

        if (!"COMPLETED".equals(payment.getStatus())) {
            payment.setStatus("COMPLETED");
            paymentRepository.save(payment);
        }
    }

    private Map<String, String> createRazorpayOrderFallback(BigDecimal amount, String receipt, Exception e) {
        log.error("Circuit breaker OPEN for Razorpay. Error: {}", e.getMessage());

        Map<String, String> response = new HashMap<>();
        response.put("razorpayOrderId", "order_mock_" + System.currentTimeMillis());
        response.put("currency", currency);
        response.put("amount", amount.multiply(new BigDecimal("100")).toPlainString());
        response.put("receipt", receipt);
        response.put("mock", "true");
        response.put("outcome", "CIRCUIT_OPEN");
        response.put("message", "Razorpay unavailable — circuit breaker open");
        return response;
    }

    private Tenant ensureDefaultTenant() {
        return tenantRepository.findByTenantId(DEFAULT_TENANT_ID).orElseGet(() -> {
            Tenant tenant = new Tenant();
            tenant.setTenantId(DEFAULT_TENANT_ID);
            tenant.setName("Default Tenant");
            tenant.setEmail("default@ebusiness.local");
            tenant.setStatus("ACTIVE");
            return tenantRepository.save(tenant);
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private PaymentStatusResponse mapToPaymentStatusResponse(Payment payment) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        response.setRazorpayOrderId(payment.getRazorpayOrderId());
        response.setStatus(payment.getStatus());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
