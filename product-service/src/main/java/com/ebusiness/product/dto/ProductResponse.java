package com.ebusiness.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private String productId;
    private String categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer stockQuantity;
    private String status;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
