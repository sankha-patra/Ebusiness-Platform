package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    @Query(value = """
        SELECT * FROM outbox_messages
        WHERE status = 'NEW'
        ORDER BY created_at ASC
        LIMIT 50
        """, nativeQuery = true)
    List<OutboxMessage> findBatchOfNew();
}
