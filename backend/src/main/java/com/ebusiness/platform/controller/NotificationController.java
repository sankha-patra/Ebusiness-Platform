package com.ebusiness.platform.controller;

import com.ebusiness.platform.dto.NotificationResponse;
import com.ebusiness.platform.dto.PageResponse;
import com.ebusiness.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.list(page, size));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> unread() {
        return ResponseEntity.ok(notificationService.unread());
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable String notificationId) {
        notificationService.markRead(notificationId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
