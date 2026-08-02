package com.ebusiness.order.repository;

import com.ebusiness.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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

    @EntityGraph(attributePaths = {"tenant", "payments"})
    Page<Order> findByTenant_TenantId(String tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"tenant", "payments"})
    @Query("SELECT o FROM Order o")
    Page<Order> findAllWithPayments(Pageable pageable);
}
