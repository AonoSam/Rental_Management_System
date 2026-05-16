package com.rental.rental_system.payment;

import com.rental.rental_system.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "mpesa_code")
    private String mpesaCode;

    @Column(name = "checkout_request_id")
    private String checkoutRequestId;

    @Column(name = "merchant_request_id")
    private String merchantRequestId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_month")
    private String paymentMonth; // e.g. "2025-05"

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}