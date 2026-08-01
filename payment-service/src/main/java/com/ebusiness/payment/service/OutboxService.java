package com.ebusiness.payment.service;

import com.ebusiness.payment.entity.OutboxMessage;
import com.ebusiness.payment.repository.OutboxMessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    public static final String TOPIC_PAYMENT_CONFIRMED = "payment-confirmed";
    public static final String TOPIC_ORDER_STATUS = "order-status-updates";

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueuePaymentConfirmed(String paymentId, String orderId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", "PaymentConfirmed");
        body.put("version", 1);
        body.put("paymentId", paymentId);
        body.put("orderId", orderId);
        save(TOPIC_PAYMENT_CONFIRMED, paymentId, body);
    }

    @Transactional
    public void enqueueOrderStatusChange(String tenantId, String orderId, String oldStatus, String newStatus) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("eventType", "OrderStatusChanged");
        body.put("version", 1);
        body.put("tenantId", tenantId);
        body.put("orderId", orderId);
        body.put("oldStatus", oldStatus);
        body.put("newStatus", newStatus);
        save(TOPIC_ORDER_STATUS, orderId, body);
    }

    private void save(String topic, String key, Map<String, Object> body) {
        try {
            OutboxMessage row = new OutboxMessage();
            row.setTopic(topic);
            row.setMessageKey(key);
            row.setPayload(objectMapper.writeValueAsString(body));
            row.setStatus("NEW");
            outboxMessageRepository.save(row);
            log.info("Outbox enqueued topic={} key={}", topic, key);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}
