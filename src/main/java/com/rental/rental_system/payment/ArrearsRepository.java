package com.rental.rental_system.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ArrearsRepository
        extends JpaRepository<ArrearsRecord, Long> {

    List<ArrearsRecord> findByTenantIdOrderByPaymentMonthDesc(Long tenantId);

    Optional<ArrearsRecord> findByTenantIdAndPaymentMonth(
            Long tenantId, String month);

    @Query("SELECT COALESCE(SUM(a.balanceRemaining), 0) " +
            "FROM ArrearsRecord a WHERE a.tenant.id = :tenantId " +
            "AND a.status = 'PARTIAL'")
    BigDecimal getTotalArrears(Long tenantId);
}