package com.ebusiness.payment.repository;

import com.ebusiness.payment.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    @Query(value = """
        SELECT * FROM outbox_messages
        WHERE status = 'NEW'
        ORDER BY created_at ASC
        LIMIT 50
        """, nativeQuery = true)
    List<OutboxMessage> findBatchOfNew();
}
