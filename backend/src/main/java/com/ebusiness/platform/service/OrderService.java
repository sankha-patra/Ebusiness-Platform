package com.ebusiness.platform.service;

import com.ebusiness.platform.dto.OrderItemResponse;
import com.ebusiness.platform.dto.OrderStatusResponse;
import com.ebusiness.platform.dto.OrderSummaryResponse;
import com.ebusiness.platform.dto.PageResponse;
import com.ebusiness.platform.entity.Order;
import com.ebusiness.platform.entity.OrderItem;
import com.ebusiness.platform.entity.Payment;
import com.ebusiness.platform.exception.OrderNotFoundException;
import com.ebusiness.platform.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listOrders(String tenantId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orders = (tenantId != null && !tenantId.isBlank())
            ? orderRepository.findByTenant_TenantId(tenantId, pageable)
            : orderRepository.findAll(pageable);

        List<OrderSummaryResponse> content = orders.getContent().stream()
            .map(this::mapToSummary)
            .collect(Collectors.toList());

        return new PageResponse<>(
            content,
            orders.getNumber(),
            orders.getSize(),
            orders.getTotalElements(),
            orders.getTotalPages()
        );
    }

    @Cacheable(
        value = "orderStatus",
        key = "'order:' + #tenantId + ':' + #orderId",
        unless = "#result == null"
    )
    @Transactional(readOnly = true)
    public OrderStatusResponse getOrderStatus(String tenantId, String orderId) {
        log.info("Fetching order status for tenant: {}, order: {}", tenantId, orderId);

        Order order = orderRepository.findByTenantAndId(tenantId, orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        return mapToOrderStatusResponse(order);
    }

    @CacheEvict(
        value = "orderStatus",
        key = "'order:' + #tenantId + ':' + #orderId"
    )
    @Transactional
    public void updateOrderStatus(String tenantId, String orderId, String newStatus) {
        log.info("Updating order status for tenant: {}, order: {} to {}", tenantId, orderId, newStatus);

        Order order = orderRepository.findByTenantAndId(tenantId, orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        orderRepository.save(order);

        kafkaProducerService.publishOrderStatusChangeEvent(tenantId, orderId, oldStatus, newStatus);
    }

    @CacheEvict(value = "orderStatus", key = "'order:' + #tenantId + ':' + #orderId")
    public void handlePaymentWebhook(String tenantId, String orderId) {
        log.info("Evicting cache for order {} due to payment webhook", orderId);
    }

    @Transactional
    public void markPaidFromEvent(String orderId) {
        orderRepository.findByOrderId(orderId).ifPresent(order -> {
            if (!"PAID".equals(order.getStatus())) {
                order.setStatus("PAID");
                orderRepository.save(order);
            }
            String tenantId = order.getTenant() != null ? order.getTenant().getTenantId() : "default";
            handlePaymentWebhook(tenantId, orderId);
        });
    }

    private OrderSummaryResponse mapToSummary(Order order) {
        String paymentStatus = "NONE";
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            paymentStatus = order.getPayments().iterator().next().getStatus();
        }
        String tenantId = order.getTenant() != null ? order.getTenant().getTenantId() : null;
        return new OrderSummaryResponse(
            order.getOrderId(),
            tenantId,
            order.getStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            paymentStatus,
            order.getRazorpayOrderId(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }

    private OrderStatusResponse mapToOrderStatusResponse(Order order) {
        List<OrderItemResponse> items = order.getItems() == null ? List.of() : order.getItems().stream()
            .map(this::mapToOrderItemResponse)
            .collect(Collectors.toList());

        String paymentStatus = "PENDING";
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().iterator().next();
            paymentStatus = payment.getStatus();
        }

        return new OrderStatusResponse(
            order.getOrderId(),
            order.getTenant().getTenantId(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            paymentStatus,
            order.getCreatedAt(),
            order.getUpdatedAt(),
            items
        );
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        String productId = item.getProduct() != null ? item.getProduct().getProductId() : null;
        String productName = item.getProduct() != null ? item.getProduct().getName() : "Item";
        return new OrderItemResponse(
            productId,
            productName,
            item.getQuantity(),
            item.getUnitPrice(),
            item.getTotalPrice()
        );
    }
}
