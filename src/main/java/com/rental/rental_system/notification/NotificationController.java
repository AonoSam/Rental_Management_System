package com.rental.rental_system.notification;

import com.rental.rental_system.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    // Get my notifications
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                notificationService.getMyNotifications(currentUser.getId()));
    }

    // Unread count
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(Map.of(
                "count", notificationService.getUnreadCount(currentUser.getId())));
    }

    // Mark single as read
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    // Mark all as read
    @PutMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllRead(
            @AuthenticationPrincipal User currentUser) {
        notificationService.markAllRead(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    // Delete one
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        notificationService.delete(id, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
    // Admin sends a notice to specific users or all
    @PostMapping("/send-notice")
    public ResponseEntity<Map<String, Object>> sendNotice(
            @RequestBody Map<String, Object> body) {

        String title   = (String) body.get("title");
        String message = (String) body.get("message");
        String target  = (String) body.get("target"); // "ALL", "TENANTS", "CARETAKERS"

        notificationService.sendAdminNotice(title, message, target);
        return ResponseEntity.ok(Map.of("message", "Notice sent successfully"));
    }
    // Delete all notifications
    @DeleteMapping("/delete-all")
    public ResponseEntity<Map<String, Object>> deleteAll(
            @AuthenticationPrincipal User currentUser) {
        notificationService.deleteAll(currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications deleted"));
    }
}