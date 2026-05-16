package com.rental.rental_system.property;

import com.rental.rental_system.config.UserDetailsServiceImpl;
import com.rental.rental_system.property.dto.*;
import com.rental.rental_system.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PropertyController {

    private final PropertyService propertyService;

    // ── Dashboard stats ──────────────────────────────────
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> dashboardStats() {
        return ResponseEntity.ok(propertyService.getDashboardStats());
    }

    // ── Properties ───────────────────────────────────────
    @GetMapping("/properties")
    public ResponseEntity<List<PropertyResponse>> listProperties() {
        return ResponseEntity.ok(propertyService.getAllProperties());
    }

    @GetMapping("/properties/{id}")
    public ResponseEntity<PropertyResponse> getProperty(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getProperty(id));
    }

    @PostMapping("/properties")
    public ResponseEntity<PropertyResponse> createProperty(
            @Valid @RequestBody PropertyRequest req,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                propertyService.createProperty(req, currentUser.getId()));
    }

    @PutMapping("/properties/{id}")
    public ResponseEntity<PropertyResponse> updateProperty(
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest req) {
        return ResponseEntity.ok(propertyService.updateProperty(id, req));
    }

    @DeleteMapping("/properties/{id}")
    public ResponseEntity<Map<String, String>> deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.ok(Map.of("message", "Property deleted"));
    }

    // ── Units ────────────────────────────────────────────
    @GetMapping("/properties/{propertyId}/units")
    public ResponseEntity<List<UnitResponse>> listUnits(@PathVariable Long propertyId) {
        return ResponseEntity.ok(propertyService.getUnitsByProperty(propertyId));
    }

    @PostMapping("/properties/{propertyId}/units")
    public ResponseEntity<UnitResponse> createUnit(
            @PathVariable Long propertyId,
            @Valid @RequestBody UnitRequest req) {
        return ResponseEntity.ok(propertyService.createUnit(propertyId, req));
    }

    @PutMapping("/units/{unitId}")
    public ResponseEntity<UnitResponse> updateUnit(
            @PathVariable Long unitId,
            @Valid @RequestBody UnitRequest req) {
        return ResponseEntity.ok(propertyService.updateUnit(unitId, req));
    }

    @DeleteMapping("/units/{unitId}")
    public ResponseEntity<Map<String, String>> deleteUnit(@PathVariable Long unitId) {
        propertyService.deleteUnit(unitId);
        return ResponseEntity.ok(Map.of("message", "Unit deleted"));
    }

    @GetMapping("/units/vacant")
    public ResponseEntity<List<UnitResponse>> getVacantUnits() {
        return ResponseEntity.ok(propertyService.getVacantUnits());
    }
}