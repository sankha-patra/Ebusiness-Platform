package com.ebusiness.platform.repository;

import com.ebusiness.platform.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByCategoryId(String categoryId);
    
    List<Category> findByStatus(String status);
    
    boolean existsByCategoryId(String categoryId);
}
