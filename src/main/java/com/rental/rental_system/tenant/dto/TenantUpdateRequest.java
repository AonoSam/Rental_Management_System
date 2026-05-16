package com.rental.rental_system.tenant.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TenantUpdateRequest {
    private String name;
    private String phone;
    private String nationalId;
    private String emergencyContact;
    private String emergencyPhone;
    private Long unitId;
    private LocalDate leaseStart;
    private LocalDate leaseEnd;
    private String status;
}