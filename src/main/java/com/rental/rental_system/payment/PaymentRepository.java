package com.rental.rental_system.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Payment> findAllByOrderByCreatedAtDesc();

    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'PAID' AND p.paymentMonth = :month")
    BigDecimal sumSuccessfulPaymentsByMonth(String month);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'PAID'")
    long countSuccessfulPayments();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = :status")
    long countByTenantIdAndStatus(Long tenantId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.tenant.id = :tenantId AND p.status = 'PAID'")
    java.math.BigDecimal sumSuccessfulPaymentsByTenantId(Long tenantId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'PAID' " +
            "AND FUNCTION('YEAR', p.createdAt) = :year")
    BigDecimal sumSuccessfulPaymentsByYear(int year);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.status = 'PAID'")
    BigDecimal sumAllSuccessfulPayments();

    boolean existsByTenantIdAndPaymentMonthAndStatus(Long tenantId, String paymentMonth, PaymentStatus status);

    List<Payment> findByStatusOrderByCreatedAtDesc(PaymentStatus status);

    List<Payment> findByPaymentMonthAndStatus(String month, PaymentStatus status);
}