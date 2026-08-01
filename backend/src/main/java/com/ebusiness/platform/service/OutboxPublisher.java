package com.ebusiness.platform.service;

import com.ebusiness.platform.entity.OutboxMessage;
import com.ebusiness.platform.event.OrderStatusChangeEvent;
import com.ebusiness.platform.event.PaymentConfirmedEvent;
import com.ebusiness.platform.repository.OutboxMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls NEW outbox rows and publishes to Kafka.
 * Disabled when payment-service owns the outbox publisher (avoid double-publish).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ebusiness.outbox.publisher-enabled", havingValue = "true")
public class OutboxPublisher {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.enabled:true}")
    private boolean kafkaEnabled;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:2000}")
    @Transactional
    public void publishPending() {
        if (!kafkaEnabled || kafkaTemplate == null) {
            return;
        }

        List<OutboxMessage> batch = outboxMessageRepository.findBatchOfNew();
        for (OutboxMessage row : batch) {
            try {
                JsonNode json = objectMapper.readTree(row.getPayload());
                Object event = toKafkaPayload(row.getTopic(), json);
                kafkaTemplate.send(row.getTopic(), row.getMessageKey(), event).get();
                row.setStatus("SENT");
                row.setSentAt(LocalDateTime.now());
                row.setLastError(null);
                outboxMessageRepository.save(row);
                log.info("Outbox SENT id={} topic={} key={}", row.getId(), row.getTopic(), row.getMessageKey());
            } catch (Exception e) {
                row.setLastError(e.getMessage());
                outboxMessageRepository.save(row);
                log.error("Outbox publish error id={} topic={} (stays NEW for retry): {}",
                    row.getId(), row.getTopic(), e.getMessage());
            }
        }
    }

    /** Map thin JSON → existing consumer types (no common-dto module). */
    private Object toKafkaPayload(String topic, JsonNode json) {
        if (OutboxService.TOPIC_PAYMENT_CONFIRMED.equals(topic)) {
            return new PaymentConfirmedEvent(
                text(json, "paymentId"),
                text(json, "orderId")
            );
        }
        if (OutboxService.TOPIC_ORDER_STATUS.equals(topic)) {
            OrderStatusChangeEvent event = new OrderStatusChangeEvent();
            event.setTenantId(text(json, "tenantId"));
            event.setOrderId(text(json, "orderId"));
            event.setOldStatus(text(json, "oldStatus"));
            event.setNewStatus(text(json, "newStatus"));
            event.setTimestamp(LocalDateTime.now());
            return event;
        }
        return json.toString();
    }

    private String text(JsonNode json, String field) {
        JsonNode n = json.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }
}
