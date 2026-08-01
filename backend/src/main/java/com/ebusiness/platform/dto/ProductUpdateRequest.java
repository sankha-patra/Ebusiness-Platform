package com.ebusiness.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateRequest {
    
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String status;
}
