package com.rental.rental_system.tenant;

import com.rental.rental_system.notification.NotificationService;
import com.rental.rental_system.property.*;
import com.rental.rental_system.tenant.dto.*;
import com.rental.rental_system.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rental.rental_system.payment.PaymentRepository;
import com.rental.rental_system.payment.PaymentStatus;
import com.rental.rental_system.maintenance.MaintenanceRepository ;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository     tenantRepository;
    private final UserRepository       userRepository;
    private final UnitRepository       unitRepository;
    private final PasswordEncoder      passwordEncoder;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final MaintenanceRepository maintenanceRepository;

    // ── Get all tenants ──────────────────────────────────
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TenantResponse> getActiveTenants() {
        return tenantRepository.findByStatus(TenantStatus.ACTIVE).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TenantResponse getTenant(Long id) {
        return toResponse(tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found")));
    }

    // ── Register new tenant ──────────────────────────────
    @Transactional
    public TenantResponse registerTenant(TenantRequest req) {
        // Check email not taken
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already registered");

        // Check unit exists and is vacant
        Unit unit = unitRepository.findById(req.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        if (unit.getStatus() == UnitStatus.OCCUPIED)
            throw new RuntimeException("Unit " + unit.getHouseNumber() + " is already occupied");

        // Create user account for tenant
        String rawPassword = (req.getPassword() != null && !req.getPassword().trim().isEmpty())
                ? req.getPassword().trim()
                : "tenant123";

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.TENANT)
                .active(true)
                .build();
        userRepository.save(user);

        // Create tenant record
        Tenant tenant = Tenant.builder()
                .user(user)
                .unit(unit)
                .nationalId(req.getNationalId())
                .emergencyContact(req.getEmergencyContact())
                .emergencyPhone(req.getEmergencyPhone())
                .leaseStart(req.getLeaseStart())
                .leaseEnd(req.getLeaseEnd())
                .status(TenantStatus.ACTIVE)
                .build();
        tenantRepository.save(tenant);

        notificationService.welcomeTenant(user);

        // Mark unit as occupied
        unit.setStatus(UnitStatus.OCCUPIED);
        unitRepository.save(unit);

        return toResponse(tenant);
    }

    // ── Update tenant ────────────────────────────────────
    @Transactional
    public TenantResponse updateTenant(Long id, TenantUpdateRequest req) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Update user info
        User user = tenant.getUser();
        if (req.getName()  != null) user.setName(req.getName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        userRepository.save(user);

        // Update tenant fields
        if (req.getNationalId()       != null) tenant.setNationalId(req.getNationalId());
        if (req.getEmergencyContact() != null) tenant.setEmergencyContact(req.getEmergencyContact());
        if (req.getEmergencyPhone()   != null) tenant.setEmergencyPhone(req.getEmergencyPhone());
        if (req.getLeaseStart()       != null) tenant.setLeaseStart(req.getLeaseStart());
        if (req.getLeaseEnd()         != null) tenant.setLeaseEnd(req.getLeaseEnd());

        // Change unit if requested
        if (req.getUnitId() != null &&
                (tenant.getUnit() == null || !req.getUnitId().equals(tenant.getUnit().getId()))) {

            // Free old unit
            if (tenant.getUnit() != null) {
                Unit oldUnit = tenant.getUnit();
                oldUnit.setStatus(UnitStatus.VACANT);
                unitRepository.save(oldUnit);
            }

            // Assign new unit
            Unit newUnit = unitRepository.findById(req.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Unit not found"));
            if (newUnit.getStatus() == UnitStatus.OCCUPIED)
                throw new RuntimeException("Unit " + newUnit.getHouseNumber() + " is already occupied");

            newUnit.setStatus(UnitStatus.OCCUPIED);
            unitRepository.save(newUnit);
            tenant.setUnit(newUnit);
        }

        // Change status if requested
        if (req.getStatus() != null) {
            tenant.setStatus(TenantStatus.valueOf(req.getStatus().toUpperCase()));
        }

        return toResponse(tenantRepository.save(tenant));
    }

    // ── Vacate tenant ────────────────────────────────────
    @Transactional
    public TenantResponse vacateTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Free the unit
        if (tenant.getUnit() != null) {
            Unit unit = tenant.getUnit();
            unit.setStatus(UnitStatus.VACANT);
            unitRepository.save(unit);
            tenant.setUnit(null);
        }

        // Record actual vacate date automatically
        tenant.setLeaseEnd(java.time.LocalDate.now());
        tenant.setStatus(TenantStatus.VACATED);
        return toResponse(tenantRepository.save(tenant));
    }

    // ── Dashboard stats ──────────────────────────────────
    public java.util.Map<String, Object> getTenantStats() {
        return java.util.Map.of(
                "totalTenants",  tenantRepository.count(),
                "activeTenants", tenantRepository.countActiveTenants()
        );
    }

    // ── Mapper ───────────────────────────────────────────
    private TenantResponse toResponse(Tenant t) {
        TenantResponse.TenantResponseBuilder builder = TenantResponse.builder()
                .id(t.getId())
                .userId(t.getUser().getId())
                .name(t.getUser().getName())
                .email(t.getUser().getEmail())
                .phone(t.getUser().getPhone())
                .nationalId(t.getNationalId())
                .emergencyContact(t.getEmergencyContact())
                .emergencyPhone(t.getEmergencyPhone())
                .leaseStart(t.getLeaseStart())
                .leaseEnd(t.getLeaseEnd())
                .status(t.getStatus().name())
                .createdAt(t.getCreatedAt());

        if (t.getUnit() != null) {
            builder
                    .unitId(t.getUnit().getId())
                    .unitNumber(t.getUnit().getHouseNumber())
                    .propertyName(t.getUnit().getProperty().getName())
                    .category(t.getUnit().getCategory().name())
                    .rentAmount(t.getUnit().getRentAmount());
        }

        return builder.build();
    }

    public TenantResponse getTenantByUserId(Long userId) {
        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tenant profile not found"));
        return toResponse(tenant);
    }

    public java.util.Map<String, Object> getTenantDashboard(Long userId) {
        Tenant tenant = tenantRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Tenant profile not found"));

        // Get payment summary
        long totalPayments = paymentRepository.countByTenantIdAndStatus(
                tenant.getId(), com.rental.rental_system.payment.PaymentStatus.SUCCESS);
        java.math.BigDecimal totalPaid = paymentRepository
                .sumSuccessfulPaymentsByTenantId(tenant.getId());

        // Current month payment status
        String thisMonth = java.time.YearMonth.now().toString();
        boolean paidThisMonth = paymentRepository
                .existsByTenantIdAndPaymentMonthAndStatus(
                        tenant.getId(), thisMonth,
                        com.rental.rental_system.payment.PaymentStatus.SUCCESS);

        return java.util.Map.of(
                "tenant",        toResponse(tenant),
                "totalPayments", totalPayments,
                "totalPaid",     totalPaid != null ? totalPaid : java.math.BigDecimal.ZERO,
                "paidThisMonth", paidThisMonth,
                "currentMonth",  thisMonth
        );
    }

    // caretaker

    public java.util.Map<String, Object> getCaretakerOverview() {
        long totalUnits    = unitRepository.count();
        long occupiedUnits = unitRepository.findAll().stream()
                .filter(u -> u.getStatus() == com.rental.rental_system.property.UnitStatus.OCCUPIED)
                .count();
        long vacantUnits   = totalUnits - occupiedUnits;
        long activeTenants = tenantRepository.countActiveTenants();
        long pendingMaintenance = maintenanceRepository
                .countByStatus(com.rental.rental_system.maintenance.MaintenanceStatus.PENDING);

        return java.util.Map.of(
                "totalUnits",        totalUnits,
                "occupiedUnits",     occupiedUnits,
                "vacantUnits",       vacantUnits,
                "activeTenants",     activeTenants,
                "pendingMaintenance",pendingMaintenance
        );
    }
}