package com.rental.rental_system.notification;

import com.rental.rental_system.payment.ArrearsStatus;
import com.rental.rental_system.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rental.rental_system.user.UserRepository;
import com.rental.rental_system.tenant.TenantRepository;
import com.rental.rental_system.payment.ArrearsRepository;
import com.rental.rental_system.notification.NotificationService;

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
    private final TenantRepository tenantRepository;
    private final ArrearsRepository arrearsRepository;

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

    // ── Payment received — notify tenant AND all admins ──
    public void paymentReceived(User tenantUser, String amount,
                                String code, String month) {
        // Notify tenant
        send(tenantUser, NotificationType.PAYMENT_RECEIVED,
                "✅ Payment Confirmed",
                "Your rent payment of KES " + amount + " for " + month +
                        " has been received. M-Pesa code: " + code);

        // Notify all admins
        notifyAllAdmins(NotificationType.PAYMENT_RECEIVED,
                "💰 Rent Payment Received",
                tenantUser.getName() + " paid KES " + amount +
                        " for " + month + ". M-Pesa code: " + code);
    }

    // ── Payment failed — notify tenant AND all admins ────
    public void paymentFailed(User tenantUser, String amount,
                              String reason) {
        // Notify tenant
        send(tenantUser, NotificationType.PAYMENT_FAILED,
                "❌ Payment Failed",
                "Your payment of KES " + amount + " failed. " +
                        (reason != null ? "Reason: " + reason : "Please try again."));

        // Notify all admins
        notifyAllAdmins(NotificationType.PAYMENT_FAILED,
                "❌ Payment Failed",
                tenantUser.getName() + "'s payment of KES " + amount +
                        " failed. " + (reason != null ? reason : ""));
    }

    // ── Maintenance reported — notify admin AND caretaker ─
    public void maintenanceReported(User tenantUser, String issue,
                                    String propertyName,
                                    String unitNumber,
                                    Long propertyId) {
        String msg = tenantUser.getName() + " (Unit " + unitNumber +
                ", " + propertyName + ") reported: " + issue;

        // Notify all admins
        notifyAllAdmins(NotificationType.MAINTENANCE_UPDATE,
                "🔧 New Maintenance Request", msg);

        // Notify assigned caretaker for this property
        if (propertyId != null) {
            userRepository.findByRole(
                            com.rental.rental_system.user.Role.CARETAKER)
                    .stream()
                    .filter(c -> c.getAssignedProperty() != null
                            && c.getAssignedProperty().getId().equals(propertyId))
                    .forEach(caretaker -> send(caretaker,
                            NotificationType.MAINTENANCE_UPDATE,
                            "🔧 New Maintenance Request", msg));
        }
    }

    // ── Maintenance updated — notify tenant ──────────────
    public void maintenanceUpdated(User tenantUser, String issue,
                                   String status, String notes) {
        send(tenantUser, NotificationType.MAINTENANCE_UPDATE,
                "🔧 Maintenance Update",
                "Your request '" + issue + "' is now: " +
                        status.replace("_", " ") +
                        (notes != null && !notes.isEmpty() ? ". Note: " + notes : ""));
    }

    // ── Welcome new tenant — notify tenant AND admin ─────
    public void welcomeTenant(User tenantUser) {
        send(tenantUser, NotificationType.ACCOUNT_CREATED,
                "🎉 Welcome to RentEase",
                "Your account has been created. You can now log in " +
                        "and pay rent.");

        notifyAllAdmins(NotificationType.ACCOUNT_CREATED,
                "👤 New Tenant Registered",
                tenantUser.getName() + " (" + tenantUser.getEmail() +
                        ") has been registered as a tenant.");
    }

    // ── Rent reminder ─────────────────────────────────────
    public void rentReminder(User user, String amount, String month) {
        send(user, NotificationType.RENT_REMINDER,
                "⚠️ Rent Due",
                "Your rent of KES " + amount + " for " + month +
                        " is due. Please pay via M-Pesa.");
    }

    // ── Internal — send to all admins ────────────────────
    private void notifyAllAdmins(NotificationType type,
                                 String title, String message) {
        userRepository.findByRole(
                        com.rental.rental_system.user.Role.ADMIN)
                .forEach(admin -> send(admin, type, title, message));
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

    // Runs at 11:59 PM on the last day of every month
    @Scheduled(cron = "0 59 23 L * *")
    @Transactional
    public void markOverduePayments() {
        String thisMonth = java.time.YearMonth.now().toString();
        log.info("Running overdue check for month: {}", thisMonth);

        tenantRepository.findByStatus(
                        com.rental.rental_system.tenant.TenantStatus.ACTIVE)
                .forEach(tenant -> {
                    // Check if tenant has unpaid/partial for this month
                    arrearsRepository
                            .findByTenantIdAndPaymentMonth(
                                    tenant.getId(), thisMonth)
                            .ifPresent(ar -> {
                                if (ar.getStatus() == ArrearsStatus.PARTIAL) {
                                    ar.setStatus(ArrearsStatus.OVERDUE);
                                    arrearsRepository.save(ar);

                                    // Send overdue notification
                                    send(tenant.getUser(),
                                            com.rental.rental_system.notification
                                                    .NotificationType.RENT_REMINDER,
                                            "🔴 Rent Overdue",
                                            "Your rent for " + thisMonth +
                                                    " is overdue. Balance: KES " +
                                                    ar.getBalanceRemaining().toPlainString());
                                }
                            });

                    // If no payment record at all — fully overdue
                    boolean hasAnyPayment = arrearsRepository
                            .findByTenantIdAndPaymentMonth(
                                    tenant.getId(), thisMonth)
                            .isPresent();

                    if (!hasAnyPayment && tenant.getUnit() != null) {
                        java.math.BigDecimal rentAmount =
                                tenant.getUnit().getRentAmount();

                        // Create overdue record
                        arrearsRepository.save(
                                com.rental.rental_system.payment.ArrearsRecord.builder()
                                        .tenant(tenant)
                                        .paymentMonth(thisMonth)
                                        .rentAmount(rentAmount)
                                        .amountPaid(java.math.BigDecimal.ZERO)
                                        .arrearsAmount(rentAmount)
                                        .carriedArrears(
                                                tenant.getArrearsBalance() != null
                                                        ? tenant.getArrearsBalance()
                                                        : java.math.BigDecimal.ZERO)
                                        .totalDue(rentAmount)
                                        .balanceRemaining(rentAmount)
                                        .status(ArrearsStatus.OVERDUE)
                                        .build());

                        // Add to tenant's arrears balance
                        tenant.setArrearsBalance(
                                (tenant.getArrearsBalance() != null
                                        ? tenant.getArrearsBalance()
                                        : java.math.BigDecimal.ZERO)
                                        .add(rentAmount));
                        tenantRepository.save(tenant);

                        send(tenant.getUser(),
                                com.rental.rental_system.notification
                                        .NotificationType.RENT_REMINDER,
                                "🔴 Rent Overdue",
                                "Your rent of KES " +
                                        rentAmount.toPlainString() +
                                        " for " + thisMonth + " was not paid.");
                    }
                });
    }
}