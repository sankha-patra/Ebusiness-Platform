package com.ebusiness.payment.service;

import com.ebusiness.payment.entity.OutboxMessage;
import com.ebusiness.payment.event.OrderStatusChangeEvent;
import com.ebusiness.payment.event.PaymentConfirmedEvent;
import com.ebusiness.payment.repository.OutboxMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
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
                log.error("Outbox publish error id={} topic={} (stays NEW): {}",
                    row.getId(), row.getTopic(), e.getMessage());
            }
        }
    }

    private Object toKafkaPayload(String topic, JsonNode json) {
        if (OutboxService.TOPIC_PAYMENT_CONFIRMED.equals(topic)) {
            return new PaymentConfirmedEvent(text(json, "paymentId"), text(json, "orderId"));
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
