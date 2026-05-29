package com.rental.rental_system.maintenance;

import com.rental.rental_system.maintenance.dto.*;
import com.rental.rental_system.notification.NotificationService;
import com.rental.rental_system.tenant.Tenant;
import com.rental.rental_system.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rental.rental_system.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final TenantRepository      tenantRepository;
    private final NotificationService   notificationService;
    private final UserRepository        userRepository;

    // ── Get all requests (admin/caretaker) ───────────────
    public List<MaintenanceResponse> getAll() {
        return maintenanceRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Get tenant's own requests ────────────────────────
    public List<MaintenanceResponse> getByTenantUserId(Long userId) {
        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
        return maintenanceRepository
                .findByTenantIdOrderByCreatedAtDesc(tenant.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Create maintenance request ───────────────────────
    @Transactional
    public MaintenanceResponse create(MaintenanceRequestDto req, Long userId) {
        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        MaintenanceRequest mr = MaintenanceRequest.builder()
                .tenant(tenant)
                .unit(tenant.getUnit())
                .issue(req.getIssue())
                .description(req.getDescription())
                .priority(req.getPriority() != null
                        ? MaintenancePriority.valueOf(
                        req.getPriority().toUpperCase())
                        : MaintenancePriority.MEDIUM)
                .status(MaintenanceStatus.PENDING)
                .build();

        maintenanceRepository.save(mr);

        // Notify admin + assigned caretaker
        notificationService.maintenanceReported(
                tenant.getUser(),
                req.getIssue(),
                tenant.getUnit() != null
                        ? tenant.getUnit().getProperty().getName()
                        : "Unknown",
                tenant.getUnit() != null
                        ? tenant.getUnit().getHouseNumber()
                        : "Unknown",
                tenant.getUnit() != null
                        ? tenant.getUnit().getProperty().getId()
                        : null
        );

        return toResponse(mr);
    }

    // ── Update status ────────────────────────────────────
    @Transactional
    public MaintenanceResponse updateStatus(Long id,
                                            String status,
                                            String notes) {
        MaintenanceRequest mr = maintenanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        mr.setStatus(MaintenanceStatus.valueOf(status.toUpperCase()));

        if (notes != null) mr.setNotes(notes);

        if (mr.getStatus() == MaintenanceStatus.COMPLETED)
            mr.setResolvedAt(LocalDateTime.now());

        MaintenanceRequest updated = maintenanceRepository.save(mr);

        // Notify tenant of status change
        notificationService.maintenanceUpdated(
                updated.getTenant().getUser(),
                updated.getIssue(),
                updated.getStatus().name(),
                notes
        );

        return toResponse(updated);
    }

    // ── Stats ────────────────────────────────────────────
    public Map<String, Object> getStats() {
        return Map.of(
                "pending",    maintenanceRepository
                        .countByStatus(MaintenanceStatus.PENDING),
                "inProgress", maintenanceRepository
                        .countByStatus(MaintenanceStatus.IN_PROGRESS),
                "completed",  maintenanceRepository
                        .countByStatus(MaintenanceStatus.COMPLETED)
        );
    }

    // ── Mapper ───────────────────────────────────────────
    private MaintenanceResponse toResponse(MaintenanceRequest mr) {
        return MaintenanceResponse.builder()
                .id(mr.getId())
                .tenantId(mr.getTenant().getId())
                .tenantName(mr.getTenant().getUser().getName())
                .unitNumber(mr.getUnit() != null
                        ? mr.getUnit().getHouseNumber() : null)
                .propertyName(mr.getUnit() != null
                        ? mr.getUnit().getProperty().getName() : null)
                .issue(mr.getIssue())
                .description(mr.getDescription())
                .status(mr.getStatus().name())
                .priority(mr.getPriority() != null
                        ? mr.getPriority().name() : null)
                .notes(mr.getNotes())
                .createdAt(mr.getCreatedAt())
                .resolvedAt(mr.getResolvedAt())
                .build();
    }
}