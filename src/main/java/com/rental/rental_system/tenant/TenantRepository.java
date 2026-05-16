package com.rental.rental_system.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    List<Tenant> findByStatus(TenantStatus status);
    Optional<Tenant> findByUserId(Long userId);
    Optional<Tenant> findByUnitId(Long unitId);
    boolean existsByUnitIdAndStatus(Long unitId, TenantStatus status);

    @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = 'ACTIVE'")
    long countActiveTenants();
}