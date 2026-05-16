package com.rental.rental_system.property;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByOwnerId(Long ownerId);

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.property.id = :propertyId AND u.status = 'VACANT'")
    long countVacantUnits(Long propertyId);

    @Query("SELECT COUNT(u) FROM Unit u WHERE u.property.id = :propertyId AND u.status = 'OCCUPIED'")
    long countOccupiedUnits(Long propertyId);
}