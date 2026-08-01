package com.ebusiness.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Transactional outbox — write event row in same TX as payment/order state.
 * A poller publishes to Kafka so we never lose events if Kafka is briefly down mid-request.
 */
@Entity
@Table(name = "outbox_messages", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 128)
    private String messageKey;

    /** Thin JSON only — never a shared fat DTO blob. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 32)
    private String status; // NEW, SENT, FAILED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
