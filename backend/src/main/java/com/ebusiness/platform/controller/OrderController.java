package com.ebusiness.platform.controller;

import com.ebusiness.platform.dto.OrderStatusResponse;
import com.ebusiness.platform.dto.OrderSummaryResponse;
import com.ebusiness.platform.dto.PageResponse;
import com.ebusiness.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryResponse>> listOrders(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/v1/orders page={} size={} tenant={}", page, size, tenantId);
        return ResponseEntity.ok(orderService.listOrders(tenantId, page, size));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String orderId) {
        
        log.info("GET /api/v1/orders/{}/status for tenant: {}", orderId, tenantId);
        OrderStatusResponse response = orderService.getOrderStatus(tenantId, orderId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String orderId,
            @RequestParam String status) {
        
        log.info("PUT /api/v1/orders/{}/status to {} for tenant: {}", orderId, status, tenantId);
        orderService.updateOrderStatus(tenantId, orderId, status);
        return ResponseEntity.noContent().build();
    }
}
