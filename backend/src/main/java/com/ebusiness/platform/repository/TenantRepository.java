package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    Optional<Tenant> findByTenantId(String tenantId);
    
    boolean existsByTenantId(String tenantId);
}
