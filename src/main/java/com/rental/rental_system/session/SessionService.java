package com.rental.rental_system.session;

import com.rental.rental_system.user.User;
import com.rental.rental_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final UserRepository    userRepository;

    // ── Record login ─────────────────────────────────────
    @Transactional
    public void recordLogin(User user, String token,
                            String ipAddress, String userAgent) {
        // Deactivate any previous active sessions for this user
        sessionRepository.findByUserIdOrderByLoggedInAtDesc(user.getId())
                .stream()
                .filter(UserSession::isActive)
                .forEach(s -> {
                    s.setActive(false);
                    s.setLoggedOutAt(LocalDateTime.now());
                    sessionRepository.save(s);
                });

        UserSession session = UserSession.builder()
                .user(user)
                .tokenHash(hashToken(token))
                .ipAddress(ipAddress)
                .deviceInfo(parseDevice(userAgent))
                .active(true)
                .forceLoggedOut(false)
                .build();

        sessionRepository.save(session);
    }

    // ── Record logout ────────────────────────────────────
    @Transactional
    public void recordLogout(String token) {
        sessionRepository.findByTokenHash(hashToken(token))
                .ifPresent(s -> {
                    s.setActive(false);
                    s.setLoggedOutAt(LocalDateTime.now());
                    sessionRepository.save(s);
                });
    }

    // ── Check if token is force-logged-out ───────────────
    public boolean isForceLoggedOut(String token) {
        return sessionRepository.findByTokenHash(hashToken(token))
                .map(UserSession::isForceLoggedOut)
                .orElse(false);
    }

    // ── All active sessions ──────────────────────────────
    public List<Map<String, Object>> getAllActiveSessions() {
        return sessionRepository.findAllActiveSessions()
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    // ── All sessions (history) ───────────────────────────
    public List<Map<String, Object>> getAllSessions() {
        return sessionRepository.findByActiveOrderByLoggedInAtDesc(false)
                .stream()
                .map(this::toMap)
                .collect(Collectors.toList());
    }

    // ── Force logout a user ──────────────────────────────
    @Transactional
    public void forceLogout(Long userId) {
        sessionRepository.forceLogoutUser(userId);
        log.info("Force logged out user ID: {}", userId);
    }

    // ── Block user (disable account) ─────────────────────
    @Transactional
    public void blockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
        // Also force logout
        sessionRepository.forceLogoutUser(userId);
        log.info("Blocked user ID: {}", userId);
    }

    // ── Unblock user ─────────────────────────────────────
    @Transactional
    public void unblockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        userRepository.save(user);
        log.info("Unblocked user ID: {}", userId);
    }

    // ── Helpers ──────────────────────────────────────────
    private String hashToken(String token) {
        // Simple hash — enough to identify without storing raw JWT
        return String.valueOf(token.hashCode());
    }

    private String parseDevice(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile") || userAgent.contains("Android"))
            return "📱 Mobile";
        if (userAgent.contains("Tablet") || userAgent.contains("iPad"))
            return "📱 Tablet";
        if (userAgent.contains("Chrome"))  return "🖥 Chrome";
        if (userAgent.contains("Firefox")) return "🖥 Firefox";
        if (userAgent.contains("Safari"))  return "🖥 Safari";
        if (userAgent.contains("Edge"))    return "🖥 Edge";
        return "🖥 Desktop";
    }

    private Map<String, Object> toMap(UserSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            s.getId());
        m.put("userId",        s.getUser().getId());
        m.put("userName",      s.getUser().getName());
        m.put("userEmail",     s.getUser().getEmail());
        m.put("userRole",      s.getUser().getRole().name());
        m.put("isUserBlocked", !s.getUser().isEnabled());
        m.put("ipAddress",     s.getIpAddress());
        m.put("deviceInfo",    s.getDeviceInfo());
        m.put("loggedInAt",    s.getLoggedInAt());
        m.put("loggedOutAt",   s.getLoggedOutAt());
        m.put("active",        s.isActive());
        m.put("forceLoggedOut",s.isForceLoggedOut());
        return m;
    }
}