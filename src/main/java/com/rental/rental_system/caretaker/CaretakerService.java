package com.rental.rental_system.caretaker;

import com.rental.rental_system.maintenance.MaintenanceRepository;
import com.rental.rental_system.maintenance.MaintenanceStatus;
import com.rental.rental_system.property.*;
import com.rental.rental_system.tenant.TenantRepository;
import com.rental.rental_system.tenant.TenantStatus;
import com.rental.rental_system.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaretakerService {

    private final UserRepository        userRepository;
    private final PropertyRepository    propertyRepository;
    private final UnitRepository        unitRepository;
    private final TenantRepository      tenantRepository;
    private final MaintenanceRepository maintenanceRepository;

    // ── Get all caretakers ───────────────────────────────
    public List<Map<String, Object>> getAllCaretakers() {
        return userRepository.findByRole(Role.CARETAKER).stream()
                .map(c -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",    c.getId());
                    m.put("name",  c.getName());
                    m.put("email", c.getEmail());
                    m.put("phone", c.getPhone());
                    m.put("assignedProperty", c.getAssignedProperty() != null
                            ? Map.of(
                            "id",       c.getAssignedProperty().getId(),
                            "name",     c.getAssignedProperty().getName(),
                            "location", c.getAssignedProperty().getLocation()
                    )
                            : null);
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Assign caretaker to property ─────────────────────
    @Transactional
    public Map<String, Object> assignCaretaker(Long caretakerId, Long propertyId) {
        User caretaker = userRepository.findById(caretakerId)
                .orElseThrow(() -> new RuntimeException("Caretaker not found"));

        if (caretaker.getRole() != Role.CARETAKER)
            throw new RuntimeException("User is not a caretaker");

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        caretaker.setAssignedProperty(property);
        userRepository.save(caretaker);

        return Map.of(
                "message",   "Caretaker assigned successfully",
                "caretaker", caretaker.getName(),
                "property",  property.getName()
        );
    }

    // ── Unassign caretaker ───────────────────────────────
    @Transactional
    public Map<String, Object> unassignCaretaker(Long caretakerId) {
        User caretaker = userRepository.findById(caretakerId)
                .orElseThrow(() -> new RuntimeException("Caretaker not found"));
        caretaker.setAssignedProperty(null);
        userRepository.save(caretaker);
        return Map.of("message", "Caretaker unassigned");
    }

    // ── Get caretaker's assigned property + stats ────────
    public Map<String, Object> getMyProperty(Long userId) {
        User caretaker = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (caretaker.getAssignedProperty() == null)
            throw new RuntimeException("No property assigned");

        Property p = caretaker.getAssignedProperty();

        long totalUnits    = unitRepository.countByPropertyId(p.getId());
        long occupiedUnits = unitRepository.countByPropertyIdAndStatus(
                p.getId(), UnitStatus.OCCUPIED);
        long vacantUnits   = totalUnits - occupiedUnits;
        long pendingMaint  = maintenanceRepository
                .countByStatus(MaintenanceStatus.PENDING);

        return Map.of(
                "property", Map.of(
                        "id",       p.getId(),
                        "name",     p.getName(),
                        "location", p.getLocation()
                ),
                "totalUnits",        totalUnits,
                "occupiedUnits",     occupiedUnits,
                "vacantUnits",       vacantUnits,
                "activeTenants",     tenantRepository.countActiveTenants(),
                "pendingMaintenance",pendingMaint
        );
    }

    // ── Get units for caretaker's property only ──────────
    public List<Map<String, Object>> getMyUnits(Long userId) {
        User caretaker = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (caretaker.getAssignedProperty() == null)
            return List.of();

        return unitRepository
                .findByPropertyId(caretaker.getAssignedProperty().getId())
                .stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id",          u.getId());
                    m.put("houseNumber", u.getHouseNumber());
                    m.put("category",    u.getCategory().name());
                    m.put("rentAmount",  u.getRentAmount());
                    m.put("floor",       u.getFloor());
                    m.put("status",      u.getStatus().name());
                    m.put("propertyName",u.getProperty().getName());
                    m.put("propertyId",  u.getProperty().getId());

                    tenantRepository.findByUnitId(u.getId()).ifPresent(t -> {
                        m.put("tenantName",  t.getUser().getName());
                        m.put("tenantPhone", t.getUser().getPhone());
                        m.put("tenantId",    t.getId());
                    });
                    return m;
                })
                .collect(Collectors.toList());
    }
}