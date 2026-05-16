package com.rental.rental_system.maintenance;

import com.rental.rental_system.maintenance.dto.*;
import com.rental.rental_system.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    // Admin — all requests
    @GetMapping
    public ResponseEntity<List<MaintenanceResponse>> getAll() {
        return ResponseEntity.ok(maintenanceService.getAll());
    }

    // Tenant — their requests only
    @GetMapping("/my-requests")
    public ResponseEntity<List<MaintenanceResponse>> getMyRequests(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                maintenanceService.getByTenantUserId(currentUser.getId()));
    }

    // Tenant — submit new request
    @PostMapping
    public ResponseEntity<MaintenanceResponse> create(
            @Valid @RequestBody MaintenanceRequestDto req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                maintenanceService.create(req, currentUser.getId()));
    }

    // Admin — update status
    @PutMapping("/{id}/status")
    public ResponseEntity<MaintenanceResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(maintenanceService.updateStatus(
                id, body.get("status"), body.get("notes")));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(maintenanceService.getStats());
    }
}