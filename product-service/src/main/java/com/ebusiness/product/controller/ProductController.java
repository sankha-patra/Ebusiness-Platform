package com.ebusiness.product.controller;

import com.ebusiness.product.dto.ProductCreateRequest;
import com.ebusiness.product.dto.ProductResponse;
import com.ebusiness.product.dto.ProductUpdateRequest;
import com.ebusiness.product.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductCatalogService productCatalogService;

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String categoryId) {
        log.info("GET /api/v1/products/category/{}", categoryId);
        return ResponseEntity.ok(productCatalogService.getProductsByCategory(categoryId));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("GET /api/v1/products");
        return ResponseEntity.ok(productCatalogService.getAllProducts());
    }

    @PostMapping
    public ResponseEntity<Void> createProduct(@RequestBody ProductCreateRequest request) {
        log.info("POST /api/v1/products — {}", request.getName());
        productCatalogService.createProduct(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable String productId,
            @RequestBody ProductUpdateRequest request) {
        log.info("PUT /api/v1/products/{}", productId);
        productCatalogService.updateProduct(productId, request);
        return ResponseEntity.noContent().build();
    }
}
