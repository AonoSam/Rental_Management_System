package com.rental.rental_system.property;

import com.rental.rental_system.property.dto.*;
import com.rental.rental_system.tenant.TenantRepository;
import com.rental.rental_system.user.User;
import com.rental.rental_system.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;

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

    // Add to PropertyService fields
    private final TenantRepository tenantRepository;

    // Update getDashboardStats()
    public java.util.Map<String, Object> getDashboardStats() {
        long totalProperties = propertyRepository.count();
        long totalUnits      = unitRepository.count();
        long vacantUnits     = unitRepository.findAll().stream()
                .filter(u -> u.getStatus() == UnitStatus.VACANT).count();
        long occupiedUnits   = unitRepository.findAll().stream()
                .filter(u -> u.getStatus() == UnitStatus.OCCUPIED).count();
        long activeTenants   = tenantRepository.countActiveTenants();

        return java.util.Map.of(
                "totalProperties", totalProperties,
                "totalUnits",      totalUnits,
                "vacantUnits",     vacantUnits,
                "occupiedUnits",   occupiedUnits,
                "activeTenants",   activeTenants
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