package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByProductId(String productId);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.category.categoryId = :categoryId AND p.status = 'ACTIVE'")
    List<Product> findByCategoryId(@Param("categoryId") String categoryId);
    
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.status = 'ACTIVE'")
    List<Product> findAllActive();
    
    boolean existsByProductId(String productId);
}
