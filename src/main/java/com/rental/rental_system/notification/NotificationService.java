package com.rental.rental_system.notification;

import com.rental.rental_system.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rental.rental_system.user.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── Send a notification to a user ────────────────────
    @Transactional
    public void send(User user, NotificationType type,
                     String title, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .build());
    }

    // ── Get all notifications for logged-in user ─────────
    public List<Map<String, Object>> getMyNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",        n.getId());
                    m.put("type",      n.getType().name());
                    m.put("title",     n.getTitle());
                    m.put("message",   n.getMessage());
                    m.put("read",      n.isRead());
                    m.put("createdAt", n.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Unread count ─────────────────────────────────────
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }

    // ── Mark single notification as read ─────────────────
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    // ── Mark all as read ─────────────────────────────────
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // ── Delete a notification ─────────────────────────────
    @Transactional
    public void delete(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                notificationRepository.delete(n);
            }
        });
    }

    // ── Helpers called from other services ───────────────
    public void paymentReceived(User user, String amount,
                                String code, String month) {
        send(user, NotificationType.PAYMENT_RECEIVED,
                "✅ Payment Confirmed",
                "Your rent payment of KES " + amount + " for " + month +
                        " has been received. M-Pesa code: " + code);
    }

    public void paymentFailed(User user, String amount, String reason) {
        send(user, NotificationType.PAYMENT_FAILED,
                "❌ Payment Failed",
                "Your payment of KES " + amount + " failed. " +
                        (reason != null ? "Reason: " + reason : "Please try again."));
    }

    public void maintenanceUpdated(User user, String issue,
                                   String status, String notes) {
        send(user, NotificationType.MAINTENANCE_UPDATE,
                "🔧 Maintenance Update",
                "Your request '" + issue + "' is now: " +
                        status.replace("_", " ") +
                        (notes != null && !notes.isEmpty() ? ". Note: " + notes : ""));
    }

    public void welcomeTenant(User user) {
        send(user, NotificationType.ACCOUNT_CREATED,
                "🎉 Welcome to RentEase",
                "Your account has been created. You can now log in, " +
                        "view your unit details and pay rent.");
    }

    public void rentReminder(User user, String amount, String month) {
        send(user, NotificationType.RENT_REMINDER,
                "⚠️ Rent Due",
                "Your rent of KES " + amount + " for " + month +
                        " is due. Please pay via M-Pesa.");
    }

    // ── Admin sends notice to all tenants ─────────────────
    @Transactional
    public void sendNoticeToAll(
            List<User> users, String title, String message) {
        users.forEach(u -> send(u, NotificationType.GENERAL_NOTICE,
                title, message));
    }
    // Send notice

    public void sendAdminNotice(String title, String message, String target) {
        List<com.rental.rental_system.user.User> users;

        switch (target.toUpperCase()) {
            case "TENANTS" ->
                    users = userRepository.findByRole(
                            com.rental.rental_system.user.Role.TENANT);
            case "CARETAKERS" ->
                    users = userRepository.findByRole(
                            com.rental.rental_system.user.Role.CARETAKER);
            case "ALL" -> {
                users = new java.util.ArrayList<>();
                users.addAll(userRepository.findByRole(
                        com.rental.rental_system.user.Role.TENANT));
                users.addAll(userRepository.findByRole(
                        com.rental.rental_system.user.Role.CARETAKER));
            }
            default -> users = java.util.List.of();
        }

        users.forEach(u -> send(u, NotificationType.GENERAL_NOTICE,
                "📢 " + title, message));

        log.info("Admin notice sent to {} users (target: {})",
                users.size(), target);
    }
    @Transactional
    public void deleteAll(Long userId) {
        List<Notification> all = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(all);
    }
}