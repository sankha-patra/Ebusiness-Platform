package com.ebusiness.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Read-only product join for line items — snapshot fields preferred later; no catalog ownership. */
@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String productId;
    @Column(nullable = false)
    private String name;
}
