package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Notification> findByReadFlagFalseOrderByCreatedAtDesc();

    long countByReadFlagFalse();

    java.util.Optional<Notification> findByNotificationId(String notificationId);

    boolean existsByPaymentId(String paymentId);

    java.util.Optional<Notification> findByPaymentId(String paymentId);
}
