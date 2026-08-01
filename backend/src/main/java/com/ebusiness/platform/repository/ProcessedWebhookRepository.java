package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, Long> {
    
    boolean existsByPaymentId(String paymentId);
}
