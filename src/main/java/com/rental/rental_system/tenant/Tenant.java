package com.rental.rental_system.tenant;

import com.rental.rental_system.property.Unit;
import com.rental.rental_system.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @Column(name = "national_id")
    private String nationalId;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "emergency_phone")
    private String emergencyPhone;

    @Column(name = "lease_start")
    private LocalDate leaseStart;

    @Column(name = "lease_end")
    private LocalDate leaseEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @Column(name = "credit_balance", precision = 10, scale = 2)
    private java.math.BigDecimal creditBalance =
            java.math.BigDecimal.ZERO;

    @Column(name = "arrears_balance", precision = 10, scale = 2)
    private java.math.BigDecimal arrearsBalance =
            java.math.BigDecimal.ZERO;
}