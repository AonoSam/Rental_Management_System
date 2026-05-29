package com.rental.rental_system.repair;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;

public interface RepairRepository extends JpaRepository<RepairLog, Long> {

    List<RepairLog> findAllByOrderByRepairDateDesc();

    List<RepairLog> findByPropertyIdOrderByRepairDateDesc(Long propertyId);

    @Query("SELECT COALESCE(SUM(r.repairCost), 0) FROM RepairLog r")
    BigDecimal getTotalExpenses();

    @Query("SELECT COALESCE(SUM(r.repairCost), 0) FROM RepairLog r " +
            "WHERE FUNCTION('DATE_FORMAT', r.repairDate, '%Y-%m') = :month")
    BigDecimal getExpensesByMonth(String month);

    @Query("SELECT COALESCE(SUM(r.repairCost), 0) FROM RepairLog r " +
            "WHERE FUNCTION('YEAR', r.repairDate) = :year")
    BigDecimal getExpensesByYear(int year);

    @Query("SELECT COALESCE(SUM(r.repairCost), 0) FROM RepairLog r " +
            "WHERE r.property.id = :propertyId")
    BigDecimal getExpensesByProperty(Long propertyId);
}