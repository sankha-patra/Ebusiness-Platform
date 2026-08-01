package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByPaymentId(String paymentId);
    
    @Query("SELECT p FROM Payment p WHERE p.razorpayPaymentId = :razorpayPaymentId")
    Optional<Payment> findByRazorpayPaymentId(@Param("razorpayPaymentId") String razorpayPaymentId);
    
    @Query("SELECT p FROM Payment p WHERE p.razorpayOrderId = :razorpayOrderId")
    List<Payment> findByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId);
    
    List<Payment> findByOrder_OrderId(String orderId);
    
    boolean existsByPaymentId(String paymentId);
}
