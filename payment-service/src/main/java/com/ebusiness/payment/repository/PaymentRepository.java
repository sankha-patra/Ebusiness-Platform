package com.ebusiness.payment.repository;

import com.ebusiness.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    @Query("SELECT p FROM Payment p WHERE p.razorpayPaymentId = :razorpayPaymentId")
    Optional<Payment> findByRazorpayPaymentId(@Param("razorpayPaymentId") String razorpayPaymentId);

    @Query("SELECT p FROM Payment p WHERE p.razorpayOrderId = :razorpayOrderId")
    List<Payment> findByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId);
}
