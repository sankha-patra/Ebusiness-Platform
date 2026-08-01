package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderId(String orderId);
    
    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.tenant
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        LEFT JOIN FETCH o.payments
        WHERE o.tenant.tenantId = :tenantId AND o.orderId = :orderId
        """)
    Optional<Order> findByTenantAndId(@Param("tenantId") String tenantId, @Param("orderId") String orderId);
    
    List<Order> findByTenant_TenantId(String tenantId);

    Page<Order> findByTenant_TenantId(String tenantId, Pageable pageable);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    List<Order> findByTenant_TenantIdAndStatus(String tenantId, String status);
    
    boolean existsByOrderId(String orderId);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :before")
    List<Order> findByStatusAndCreatedAtBefore(@Param("status") String status, @Param("before") LocalDateTime before);
}
