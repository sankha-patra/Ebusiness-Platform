package com.ebusiness.product.service;

import com.ebusiness.product.dto.ProductCreateRequest;
import com.ebusiness.product.dto.ProductResponse;
import com.ebusiness.product.dto.ProductUpdateRequest;
import com.ebusiness.product.entity.Product;
import com.ebusiness.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCatalogService {

    private final ProductRepository productRepository;

    @Cacheable(value = "productsByCategory", key = "#categoryId")
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String categoryId) {
        log.info("Cache miss — fetching products for category: {}", categoryId);
        return productRepository.findByCategoryId(categoryId).stream()
            .map(this::mapToProductResponse)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "productsAll", key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        log.info("Cache miss — fetching all active products");
        return productRepository.findAllActive().stream()
            .map(this::mapToProductResponse)
            .collect(Collectors.toList());
    }

    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsByCategory", allEntries = true)
    })
    @Transactional
    public void updateProduct(String productId, ProductUpdateRequest request) {
        log.info("Updating product: {}", productId);
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setStatus(request.getStatus());
        productRepository.save(product);
    }

    @Caching(evict = {
        @CacheEvict(value = "productsAll", allEntries = true),
        @CacheEvict(value = "productsByCategory", allEntries = true)
    })
    @Transactional
    public void createProduct(ProductCreateRequest request) {
        log.info("Creating product: {}", request.getName());
        Product product = new Product();
        product.setProductId(request.getProductId());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setStockQuantity(request.getStockQuantity());
        product.setStatus(request.getStatus());
        productRepository.save(product);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return new ProductResponse(
            product.getProductId(),
            product.getCategory() != null ? product.getCategory().getCategoryId() : null,
            product.getCategory() != null ? product.getCategory().getName() : null,
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getCurrency(),
            product.getStockQuantity(),
            product.getStatus(),
            product.getImageUrl(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
