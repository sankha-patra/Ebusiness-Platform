package com.ebusiness.platform.service;

import com.ebusiness.platform.dto.NotificationResponse;
import com.ebusiness.platform.dto.PageResponse;
import com.ebusiness.platform.entity.Notification;
import com.ebusiness.platform.entity.Order;
import com.ebusiness.platform.entity.Payment;
import com.ebusiness.platform.repository.NotificationRepository;
import com.ebusiness.platform.repository.OrderRepository;
import com.ebusiness.platform.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Local stand-in for AWS SNS/SES / MSG91 / Twilio.
 * Kafka payment-confirmed → persist notification + "send" SMS (logged).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String DEMO_PHONE = "+919999999999";

    private final NotificationRepository notificationRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public NotificationResponse notifyPaymentSuccess(String orderId, String paymentId, String tenantId) {
        if (paymentId != null && notificationRepository.existsByPaymentId(paymentId)) {
            return notificationRepository.findByPaymentId(paymentId)
                .map(this::toResponse)
                .orElseThrow();
        }

        ResolvedOrder resolved = resolveOrder(orderId, paymentId);
        String message = buildMessage(resolved);

        log.info("[SMS-MOCK] to={} body=\"{}\"", DEMO_PHONE, message);

        Notification notification = new Notification();
        notification.setNotificationId("ntf-" + UUID.randomUUID().toString().substring(0, 8));
        notification.setTenantId(tenantId != null ? tenantId : "default");
        notification.setOrderId(resolved.orderId());
        notification.setPaymentId(paymentId != null ? paymentId : resolved.paymentId());
        notification.setChannel("SMS");
        notification.setRecipient(DEMO_PHONE);
        notification.setMessage(message);
        notification.setStatus("SENT");
        notification.setReadFlag(false);

        notification = notificationRepository.save(notification);
        log.info("Notification saved: id={}, orderId={}", notification.getNotificationId(), resolved.orderId());
        return toResponse(notification);
    }

    private ResolvedOrder resolveOrder(String orderId, String paymentId) {
        if (paymentId != null) {
            Optional<Payment> payment = paymentRepository.findByPaymentId(paymentId);
            if (payment.isPresent() && payment.get().getOrder() != null) {
                Order order = payment.get().getOrder();
                return new ResolvedOrder(
                    order.getOrderId(),
                    payment.get().getPaymentId(),
                    extractProductHint(order.getNotes())
                );
            }
        }

        if (orderId != null && !orderId.isBlank() && !orderId.matches("order-\\d+")) {
            Optional<Order> order = orderRepository.findByOrderId(orderId);
            if (order.isPresent()) {
                return new ResolvedOrder(
                    order.get().getOrderId(),
                    paymentId,
                    extractProductHint(order.get().getNotes())
                );
            }
            return new ResolvedOrder(orderId, paymentId, null);
        }

        // Prefer newest real order over load-test placeholders like order-0
        return orderRepository.findAll(PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")))
            .stream()
            .findFirst()
            .map(o -> new ResolvedOrder(o.getOrderId(), paymentId, extractProductHint(o.getNotes())))
            .orElse(new ResolvedOrder(orderId != null ? orderId : "unknown", paymentId, null));
    }

    private String extractProductHint(String notes) {
        if (notes == null) {
            return null;
        }
        // notes like: receipt=buy-prod-001-171234
        int buyIdx = notes.indexOf("buy-");
        if (buyIdx < 0) {
            return null;
        }
        String rest = notes.substring(buyIdx + 4);
        int end = rest.indexOf('-');
        if (end > 0) {
            return rest.substring(0, end);
        }
        return rest;
    }

    private String buildMessage(ResolvedOrder resolved) {
        if (resolved.productId() != null && !resolved.productId().isBlank()) {
            return "Your payment was successful. Order "
                + resolved.orderId()
                + " for product "
                + resolved.productId()
                + " is confirmed.";
        }
        return "Your payment was successful. Order " + resolved.orderId() + " is confirmed.";
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(int page, int size) {
        Page<Notification> result = notificationRepository.findAll(
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<NotificationResponse> content = result.getContent().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> unread() {
        return notificationRepository.findByReadFlagFalseOrderByCreatedAtDesc().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void markRead(String notificationId) {
        notificationRepository.findByNotificationId(notificationId).ifPresent(n -> {
            n.setReadFlag(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead() {
        List<Notification> unread = notificationRepository.findByReadFlagFalseOrderByCreatedAtDesc();
        unread.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
            n.getNotificationId(),
            n.getTenantId(),
            n.getOrderId(),
            n.getPaymentId(),
            n.getChannel(),
            n.getRecipient(),
            n.getMessage(),
            n.getStatus(),
            n.isReadFlag(),
            n.getCreatedAt()
        );
    }

    private record ResolvedOrder(String orderId, String paymentId, String productId) {}
}
