package com.rental.rental_system.payment;

import com.rental.rental_system.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "arrears_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrearsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "payment_month", nullable = false)
    private String paymentMonth; // e.g. "2025-05"

    @Column(name = "rent_amount", nullable = false)
    private BigDecimal rentAmount; // expected rent that month

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid; // what tenant actually paid

    @Column(name = "arrears_amount", nullable = false)
    private BigDecimal arrearsAmount; // shortfall

    @Column(name = "carried_arrears", nullable = false)
    private BigDecimal carriedArrears; // arrears brought from previous months

    @Column(name = "total_due", nullable = false)
    private BigDecimal totalDue; // rent + carried arrears

    @Column(name = "balance_remaining", nullable = false)
    private BigDecimal balanceRemaining; // still owed after payment

    @Enumerated(EnumType.STRING)
    private ArrearsStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}