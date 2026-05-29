package com.rental.rental_system.property;

import com.rental.rental_system.payment.ArrearsRepository;
import com.rental.rental_system.property.dto.*;
import com.rental.rental_system.repair.RepairRepository;
import com.rental.rental_system.tenant.TenantRepository;
import com.rental.rental_system.user.User;
import com.rental.rental_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final RepairRepository repairRepository;
    private final TenantRepository tenantRepository;
    private final ArrearsRepository arrearsRepository;
    private final com.rental.rental_system.payment.PaymentRepository paymentRepository;

    // ── Properties ─────────────────────────────────────

    public List<PropertyResponse> getAllProperties() {
        return propertyRepository.findAll().stream()
                .map(this::toPropertyResponse)
                .collect(Collectors.toList());
    }

    public PropertyResponse getProperty(Long id) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));
        return toPropertyResponse(p);
    }

    public PropertyResponse createProperty(PropertyRequest req, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Property property = Property.builder()
                .name(req.getName())
                .location(req.getLocation())
                .description(req.getDescription())
                .status(PropertyStatus.valueOf(req.getStatus().toUpperCase()))
                .totalUnits(0)
                .owner(owner)
                .build();

        return toPropertyResponse(propertyRepository.save(property));
    }

    public PropertyResponse updateProperty(Long id, PropertyRequest req) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        property.setName(req.getName());
        property.setLocation(req.getLocation());
        property.setDescription(req.getDescription());
        property.setStatus(PropertyStatus.valueOf(req.getStatus().toUpperCase()));

        return toPropertyResponse(propertyRepository.save(property));
    }

    public void deleteProperty(Long id) {
        if (!propertyRepository.existsById(id))
            throw new RuntimeException("Property not found");
        propertyRepository.deleteById(id);
    }

    // ── Units ───────────────────────────────────────────

    public List<UnitResponse> getUnitsByProperty(Long propertyId) {
        return unitRepository.findByPropertyId(propertyId).stream()
                .map(this::toUnitResponse)
                .collect(Collectors.toList());
    }

    public UnitResponse createUnit(Long propertyId, UnitRequest req) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Property not found"));

        Unit unit = Unit.builder()
                .property(property)
                .houseNumber(req.getHouseNumber())
                .category(UnitCategory.valueOf(req.getCategory().toUpperCase()))
                .rentAmount(req.getRentAmount())
                .floor(req.getFloor())
                .status(UnitStatus.valueOf(req.getStatus().toUpperCase()))
                .build();

        Unit saved = unitRepository.save(unit);

        // Update total units count on property
        property.setTotalUnits((int) unitRepository.countByPropertyId(propertyId));
        propertyRepository.save(property);

        return toUnitResponse(saved);
    }

    public UnitResponse updateUnit(Long unitId, UnitRequest req) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));

        unit.setHouseNumber(req.getHouseNumber());
        unit.setCategory(UnitCategory.valueOf(req.getCategory().toUpperCase()));
        unit.setRentAmount(req.getRentAmount());
        unit.setFloor(req.getFloor());
        unit.setStatus(UnitStatus.valueOf(req.getStatus().toUpperCase()));

        return toUnitResponse(unitRepository.save(unit));
    }

    public void deleteUnit(Long unitId) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found"));
        Long propertyId = unit.getProperty().getId();
        unitRepository.deleteById(unitId);

        // Update total units count
        Property property = propertyRepository.findById(propertyId).orElseThrow();
        property.setTotalUnits((int) unitRepository.countByPropertyId(propertyId));
        propertyRepository.save(property);
    }

    // ── Dashboard stats ─────────────────────────────────

    // Get dashboard stats

    public Map<String, Object> getDashboardStats() {
        String thisMonth = java.time.YearMonth.now().toString();
        int    thisYear  = java.time.Year.now().getValue();

        // Collections
        BigDecimal monthlyCollections = paymentRepository
                .sumSuccessfulPaymentsByMonth(thisMonth);

        BigDecimal annualCollections  = paymentRepository
                .sumSuccessfulPaymentsByYear(thisYear);

        BigDecimal totalCollections   = paymentRepository
                .sumAllSuccessfulPayments();

        // Prevent null values
        monthlyCollections = monthlyCollections != null
                ? monthlyCollections : BigDecimal.ZERO;

        annualCollections = annualCollections != null
                ? annualCollections : BigDecimal.ZERO;

        totalCollections = totalCollections != null
                ? totalCollections : BigDecimal.ZERO;

        // Expenses
        BigDecimal totalExpenses   = repairRepository.getTotalExpenses();
        BigDecimal monthlyExpenses = repairRepository.getExpensesByMonth(thisMonth);

        totalExpenses = totalExpenses != null
                ? totalExpenses : BigDecimal.ZERO;

        monthlyExpenses = monthlyExpenses != null
                ? monthlyExpenses : BigDecimal.ZERO;

        // Arrears
        BigDecimal totalArrears = tenantRepository.findAll()
                .stream()
                .map(t -> t.getArrearsBalance() != null
                        ? t.getArrearsBalance() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Net collections = total collected - total expenses
        BigDecimal netCollections = totalCollections.subtract(totalExpenses);

        // Format to 2 decimal places
        monthlyCollections = monthlyCollections.setScale(2, java.math.RoundingMode.HALF_UP);
        annualCollections  = annualCollections.setScale(2, java.math.RoundingMode.HALF_UP);
        totalCollections   = totalCollections.setScale(2, java.math.RoundingMode.HALF_UP);

        totalExpenses      = totalExpenses.setScale(2, java.math.RoundingMode.HALF_UP);
        monthlyExpenses    = monthlyExpenses.setScale(2, java.math.RoundingMode.HALF_UP);

        totalArrears       = totalArrears.setScale(2, java.math.RoundingMode.HALF_UP);

        netCollections     = netCollections.setScale(2, java.math.RoundingMode.HALF_UP);

        long totalProperties = propertyRepository.count();
        long totalUnits      = unitRepository.count();

        long occupiedUnits = unitRepository.findAll().stream()
                .filter(u -> u.getStatus() == UnitStatus.OCCUPIED)
                .count();

        long vacantUnits   = totalUnits - occupiedUnits;

        long activeTenants = tenantRepository.countActiveTenants();

        return Map.ofEntries(
                Map.entry("totalProperties",    totalProperties),
                Map.entry("totalUnits",         totalUnits),
                Map.entry("occupiedUnits",      occupiedUnits),
                Map.entry("vacantUnits",        vacantUnits),
                Map.entry("activeTenants",      activeTenants),
                Map.entry("monthlyCollections", monthlyCollections),
                Map.entry("annualCollections",  annualCollections),
                Map.entry("totalCollections",   totalCollections),
                Map.entry("totalExpenses",      totalExpenses),
                Map.entry("monthlyExpenses",    monthlyExpenses),
                Map.entry("totalArrears",       totalArrears),
                Map.entry("netCollections",     netCollections)
        );
    }

    // ── Mappers ─────────────────────────────────────────

    private PropertyResponse toPropertyResponse(Property p) {
        long vacant   = propertyRepository.countVacantUnits(p.getId());
        long occupied = propertyRepository.countOccupiedUnits(p.getId());
        return PropertyResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .location(p.getLocation())
                .description(p.getDescription())
                .totalUnits(p.getTotalUnits())
                .status(p.getStatus().name())
                .vacantUnits(vacant)
                .occupiedUnits(occupied)
                .createdAt(p.getCreatedAt())
                .build();
    }

    private UnitResponse toUnitResponse(Unit u) {
        return UnitResponse.builder()
                .id(u.getId())
                .propertyId(u.getProperty().getId())
                .propertyName(u.getProperty().getName())
                .houseNumber(u.getHouseNumber())
                .category(u.getCategory().name())
                .rentAmount(u.getRentAmount())
                .floor(u.getFloor())
                .status(u.getStatus().name())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public List<UnitResponse> getVacantUnits() {
        return unitRepository.findAll().stream()
                .filter(u -> u.getStatus() == UnitStatus.VACANT)
                .map(this::toUnitResponse)
                .collect(Collectors.toList());
    }
}