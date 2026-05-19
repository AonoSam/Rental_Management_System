package com.rental.rental_system.session;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;

    // All active sessions
    @GetMapping("/active")
    public ResponseEntity<List<Map<String, Object>>> getActive() {
        return ResponseEntity.ok(sessionService.getAllActiveSessions());
    }

    // All sessions history
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAll() {
        return ResponseEntity.ok(sessionService.getAllSessions());
    }

    // Force logout a user
    @PutMapping("/force-logout/{userId}")
    public ResponseEntity<Map<String, Object>> forceLogout(
            @PathVariable Long userId) {
        sessionService.forceLogout(userId);
        return ResponseEntity.ok(Map.of("message", "User force logged out"));
    }

    // Block user
    @PutMapping("/block/{userId}")
    public ResponseEntity<Map<String, Object>> blockUser(
            @PathVariable Long userId) {
        sessionService.blockUser(userId);
        return ResponseEntity.ok(Map.of("message", "User blocked"));
    }

    // Unblock user
    @PutMapping("/unblock/{userId}")
    public ResponseEntity<Map<String, Object>> unblockUser(
            @PathVariable Long userId) {
        sessionService.unblockUser(userId);
        return ResponseEntity.ok(Map.of("message", "User unblocked"));
    }
}