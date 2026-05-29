package com.rental.rental_system.repair;

import com.rental.rental_system.maintenance.MaintenanceRepository;
import com.rental.rental_system.property.PropertyRepository;
import com.rental.rental_system.property.UnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairService {

    private final RepairRepository       repairRepository;
    private final PropertyRepository     propertyRepository;
    private final UnitRepository         unitRepository;
    private final MaintenanceRepository  maintenanceRepository;

    // ── Get all repair logs ──────────────────────────────
    public List<Map<String, Object>> getAllRepairs() {
        return repairRepository.findAllByOrderByRepairDateDesc()
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    // ── Get repairs by property ──────────────────────────
    public List<Map<String, Object>> getRepairsByProperty(Long propertyId) {
        return repairRepository
                .findByPropertyIdOrderByRepairDateDesc(propertyId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    // ── Create repair log ────────────────────────────────
    @Transactional
    public Map<String, Object> createRepair(Map<String, Object> req) {
        var property = propertyRepository
                .findById(Long.parseLong(req.get("propertyId").toString()))
                .orElseThrow(() -> new RuntimeException("Property not found"));

        var builder = RepairLog.builder()
                .property(property)
                .description((String) req.get("description"))
                .repairedBy((String) req.get("repairedBy"))
                .repairCost(new BigDecimal(req.get("repairCost").toString()))
                .repairDate(LocalDate.parse((String) req.get("repairDate")))
                .category(RepairCategory.valueOf(
                        req.get("category").toString().toUpperCase()))
                .notes((String) req.getOrDefault("notes", null))
                .receiptNumber((String) req.getOrDefault("receiptNumber", null));

        // Link to unit if provided
        if (req.get("unitId") != null) {
            unitRepository.findById(
                            Long.parseLong(req.get("unitId").toString()))
                    .ifPresent(builder::unit);
        }

        // Link to maintenance request if provided
        if (req.get("maintenanceRequestId") != null) {
            maintenanceRepository.findById(
                            Long.parseLong(
                                    req.get("maintenanceRequestId").toString()))
                    .ifPresent(builder::maintenanceRequest);
        }

        return toMap(repairRepository.save(builder.build()));
    }

    // ── Update repair log ────────────────────────────────
    @Transactional
    public Map<String, Object> updateRepair(Long id,
                                            Map<String, Object> req) {
        RepairLog repair = repairRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Repair not found"));

        if (req.get("description") != null)
            repair.setDescription((String) req.get("description"));
        if (req.get("repairedBy") != null)
            repair.setRepairedBy((String) req.get("repairedBy"));
        if (req.get("repairCost") != null)
            repair.setRepairCost(
                    new BigDecimal(req.get("repairCost").toString()));
        if (req.get("repairDate") != null)
            repair.setRepairDate(
                    LocalDate.parse((String) req.get("repairDate")));
        if (req.get("category") != null)
            repair.setCategory(RepairCategory.valueOf(
                    req.get("category").toString().toUpperCase()));
        if (req.get("notes") != null)
            repair.setNotes((String) req.get("notes"));
        if (req.get("receiptNumber") != null)
            repair.setReceiptNumber((String) req.get("receiptNumber"));

        return toMap(repairRepository.save(repair));
    }

    // ── Delete repair log ────────────────────────────────
    @Transactional
    public void deleteRepair(Long id) {
        if (!repairRepository.existsById(id))
            throw new RuntimeException("Repair not found");
        repairRepository.deleteById(id);
    }

    // ── Expense summary ──────────────────────────────────
    public Map<String, Object> getExpenseSummary() {
        String thisMonth = java.time.YearMonth.now().toString();
        int    thisYear  = Year.now().getValue();

        BigDecimal totalExpenses        = repairRepository.getTotalExpenses();
        BigDecimal monthlyExpenses      = repairRepository.getExpensesByMonth(thisMonth);
        BigDecimal annualExpenses       = repairRepository.getExpensesByYear(thisYear);

        return Map.of(
                "totalExpenses",   totalExpenses,
                "monthlyExpenses", monthlyExpenses,
                "annualExpenses",  annualExpenses
        );
    }

    // ── Mapper ───────────────────────────────────────────
    private Map<String, Object> toMap(RepairLog r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           r.getId());
        m.put("propertyId",   r.getProperty().getId());
        m.put("propertyName", r.getProperty().getName());
        m.put("unitId",       r.getUnit() != null
                ? r.getUnit().getId() : null);
        m.put("unitNumber",   r.getUnit() != null
                ? r.getUnit().getHouseNumber() : null);
        m.put("maintenanceRequestId", r.getMaintenanceRequest() != null
                ? r.getMaintenanceRequest().getId() : null);
        m.put("description",  r.getDescription());
        m.put("repairedBy",   r.getRepairedBy());
        m.put("repairCost",   r.getRepairCost());
        m.put("repairDate",   r.getRepairDate());
        m.put("category",     r.getCategory().name());
        m.put("notes",        r.getNotes());
        m.put("receiptNumber",r.getReceiptNumber());
        m.put("createdAt",    r.getCreatedAt());
        return m;
    }
}