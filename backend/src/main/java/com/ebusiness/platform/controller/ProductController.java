package com.ebusiness.platform.controller;

import com.ebusiness.platform.dto.ProductResponse;
import com.ebusiness.platform.service.ProductCatalogService;
import com.ebusiness.platform.dto.ProductCreateRequest;
import com.ebusiness.platform.dto.ProductUpdateRequest;
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
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable String categoryId) {
        
        log.info("GET /api/v1/products/category/{}", categoryId);
        List<ProductResponse> products = productCatalogService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(products);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("GET /api/v1/products");
        List<ProductResponse> products = productCatalogService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<Void> createProduct(@RequestBody ProductCreateRequest request) {
        log.info("POST /api/v1/products - Creating product: {}", request.getName());
        productCatalogService.createProduct(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(
            @PathVariable String productId,
            @RequestBody ProductUpdateRequest request) {
        
        log.info("PUT /api/v1/products/{} - Updating product", productId);
        productCatalogService.updateProduct(productId, request);
        return ResponseEntity.noContent().build();
    }
}
