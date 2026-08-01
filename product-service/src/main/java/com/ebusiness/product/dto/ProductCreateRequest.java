package com.ebusiness.product.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {
    private String productId;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer stockQuantity;
    private String status;
}
