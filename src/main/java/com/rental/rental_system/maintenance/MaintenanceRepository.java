package com.rental.rental_system.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceRepository extends JpaRepository<MaintenanceRequest, Long> {
    List<MaintenanceRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    List<MaintenanceRequest> findAllByOrderByCreatedAtDesc();
    List<MaintenanceRequest> findByStatusOrderByCreatedAtDesc(MaintenanceStatus status);
    long countByStatus(MaintenanceStatus status);
}