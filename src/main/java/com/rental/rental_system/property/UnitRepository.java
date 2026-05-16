package com.rental.rental_system.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByPropertyId(Long propertyId);

    List<Unit> findByPropertyIdAndStatus(Long propertyId, UnitStatus status);

    long countByPropertyId(Long propertyId);

    long countByPropertyIdAndStatus(Long propertyId, UnitStatus status);
}