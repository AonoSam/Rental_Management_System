package com.rental.rental_system.report;

import com.rental.rental_system.maintenance.MaintenanceRepository;
import com.rental.rental_system.maintenance.MaintenanceStatus;
import com.rental.rental_system.payment.PaymentRepository;
import com.rental.rental_system.payment.PaymentStatus;
import com.rental.rental_system.property.PropertyRepository;
import com.rental.rental_system.property.UnitRepository;
import com.rental.rental_system.property.UnitStatus;
import com.rental.rental_system.tenant.TenantRepository;
import com.rental.rental_system.tenant.TenantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final PaymentRepository     paymentRepository;
    private final TenantRepository      tenantRepository;
    private final PropertyRepository    propertyRepository;
    private final UnitRepository        unitRepository;
    private final MaintenanceRepository maintenanceRepository;

    // ── Summary stats ────────────────────────────────────
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        String thisMonth = YearMonth.now().toString();
        String lastMonth = YearMonth.now().minusMonths(1).toString();

        BigDecimal thisMonthRevenue = paymentRepository
                .sumSuccessfulPaymentsByMonth(thisMonth);
        BigDecimal lastMonthRevenue = paymentRepository
                .sumSuccessfulPaymentsByMonth(lastMonth);

        long totalUnits    = unitRepository.count();
        long occupiedUnits = unitRepository.findAll().stream()
                .filter(u -> u.getStatus() == UnitStatus.OCCUPIED).count();
        long vacantUnits   = totalUnits - occupiedUnits;

        double occupancyRate = totalUnits > 0
                ? (double) occupiedUnits / totalUnits * 100 : 0;

        // Outstanding balances — tenants who haven't paid this month
        long activeTenants   = tenantRepository.countActiveTenants();
        long paidThisMonth   = paymentRepository
                .findByPaymentMonthAndStatus(thisMonth, PaymentStatus.SUCCESS).size();
        long unpaidThisMonth = activeTenants - paidThisMonth;

        return ResponseEntity.ok(Map.of(
                "thisMonthRevenue",  thisMonthRevenue != null ? thisMonthRevenue : BigDecimal.ZERO,
                "lastMonthRevenue",  lastMonthRevenue != null ? lastMonthRevenue : BigDecimal.ZERO,
                "totalProperties",   propertyRepository.count(),
                "totalUnits",        totalUnits,
                "occupiedUnits",     occupiedUnits,
                "vacantUnits",       vacantUnits,
                "occupancyRate",     Math.round(occupancyRate),
                "activeTenants",     activeTenants,
                "unpaidThisMonth",   unpaidThisMonth,
                "pendingMaintenance",maintenanceRepository.countByStatus(MaintenanceStatus.PENDING)
        ));
    }

    // ── Monthly revenue — last 6 months ──────────────────
    @GetMapping("/revenue/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyRevenue() {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 5; i >= 0; i--) {
            YearMonth ym      = YearMonth.now().minusMonths(i);
            String    key     = ym.toString();
            String    label   = ym.format(fmt);
            BigDecimal amount = paymentRepository.sumSuccessfulPaymentsByMonth(key);

            result.add(Map.of(
                    "month",   label,
                    "revenue", amount != null ? amount : BigDecimal.ZERO
            ));
        }
        return ResponseEntity.ok(result);
    }

    // ── Occupancy by property ────────────────────────────
    @GetMapping("/occupancy")
    public ResponseEntity<List<Map<String, Object>>> getOccupancy() {
        List<Map<String, Object>> result = new ArrayList<>();

        propertyRepository.findAll().forEach(p -> {
            long total    = unitRepository.countByPropertyId(p.getId());
            long occupied = unitRepository
                    .countByPropertyIdAndStatus(p.getId(), UnitStatus.OCCUPIED);
            long vacant   = total - occupied;

            result.add(Map.of(
                    "property", p.getName(),
                    "total",    total,
                    "occupied", occupied,
                    "vacant",   vacant,
                    "rate",     total > 0 ? Math.round((double) occupied / total * 100) : 0
            ));
        });

        return ResponseEntity.ok(result);
    }

    // ── Outstanding balances ─────────────────────────────
    @GetMapping("/outstanding")
    public ResponseEntity<List<Map<String, Object>>> getOutstanding() {
        String thisMonth = YearMonth.now().toString();
        List<Map<String, Object>> result = new ArrayList<>();

        tenantRepository.findByStatus(TenantStatus.ACTIVE).forEach(t -> {
            boolean paid = paymentRepository.existsByTenantIdAndPaymentMonthAndStatus(
                    t.getId(), thisMonth, PaymentStatus.SUCCESS);

            if (!paid) {
                Map<String, Object> row = new HashMap<>();
                row.put("tenantName",   t.getUser().getName());
                row.put("phone",        t.getUser().getPhone());
                row.put("unitNumber",   t.getUnit() != null ? t.getUnit().getHouseNumber() : "—");
                row.put("propertyName", t.getUnit() != null ? t.getUnit().getProperty().getName() : "—");
                row.put("rentAmount",   t.getUnit() != null ? t.getUnit().getRentAmount() : BigDecimal.ZERO);
                row.put("month",        thisMonth);
                result.add(row);
            }
        });

        return ResponseEntity.ok(result);
    }
}