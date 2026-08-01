package com.ebusiness.payment.repository;

import com.ebusiness.payment.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, Long> {
    boolean existsByPaymentId(String paymentId);
}
