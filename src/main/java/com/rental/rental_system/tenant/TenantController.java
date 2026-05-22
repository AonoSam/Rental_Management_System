package com.rental.rental_system.tenant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.rental.rental_system.tenant.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.rental.rental_system.user.User;



import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAll() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @GetMapping("/active")
    public ResponseEntity<List<TenantResponse>> getActive() {
        return ResponseEntity.ok(tenantService.getActiveTenants());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenant(id));
    }

    @PostMapping
    public ResponseEntity<TenantResponse> register(
            @Valid @RequestBody TenantRequest req) {
        return ResponseEntity.ok(tenantService.registerTenant(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TenantResponse> update(
            @PathVariable Long id,
            @RequestBody TenantUpdateRequest req) {
        return ResponseEntity.ok(tenantService.updateTenant(id, req));
    }

    @PutMapping("/{id}/vacate")
    public ResponseEntity<TenantResponse> vacate(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.vacateTenant(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(tenantService.getTenantStats());
    }

    // Get tenant profile by logged-in user
    @GetMapping("/my-profile")
    public ResponseEntity<TenantResponse> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(tenantService.getTenantByUserId(currentUser.getId()));
    }

    // Get tenant dashboard summary
    @GetMapping("/my-dashboard")
    public ResponseEntity<Map<String, Object>> getMyDashboard(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(tenantService.getTenantDashboard(currentUser.getId()));
    }

    // Caretaker — get all units with occupancy status
    @GetMapping("/caretaker/overview")
    public ResponseEntity<Map<String, Object>> getCaretakerOverview() {
        return ResponseEntity.ok(tenantService.getCaretakerOverview());
    }
    // Get arrears summary for a tenant
    @GetMapping("/{id}/arrears")
    public ResponseEntity<List<Map<String, Object>>> getTenantArrears(
            @PathVariable Long id) {
        return ResponseEntity.ok(tenantService.getTenantArrears(id));
    }

    // Get my arrears (tenant self)
    @GetMapping("/my-arrears")
    public ResponseEntity<Map<String, Object>> getMyArrears(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                tenantService.getMyArrears(currentUser.getId()));
    }
}