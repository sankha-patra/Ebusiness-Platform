package com.ebusiness.order.controller;

import com.ebusiness.order.dto.OrderStatusResponse;
import com.ebusiness.order.dto.OrderSummaryResponse;
import com.ebusiness.order.dto.PageResponse;
import com.ebusiness.order.service.OrderService;
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
        return ResponseEntity.ok(orderService.listOrders(tenantId, page, size));
    }

    @GetMapping("/{orderId}/status")
    public ResponseEntity<OrderStatusResponse> getOrderStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String orderId) {
        return ResponseEntity.ok(orderService.getOrderStatus(tenantId, orderId));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String orderId,
            @RequestParam String status) {
        orderService.updateOrderStatus(tenantId, orderId, status);
        return ResponseEntity.noContent().build();
    }
}
