package com.rental.rental_system.tenant.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder
public class TenantResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String nationalId;
    private String emergencyContact;
    private String emergencyPhone;

    // Unit info
    private Long unitId;
    private String unitNumber;
    private String propertyName;
    private String category;
    private java.math.BigDecimal rentAmount;

    // Lease
    private LocalDate leaseStart;
    private LocalDate leaseEnd;
    private String status;
    private LocalDateTime createdAt;
    // Arrears
    private java.math.BigDecimal arrearsBalance;
    private java.math.BigDecimal creditBalance;
}