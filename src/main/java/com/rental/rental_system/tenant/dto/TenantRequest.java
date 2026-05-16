package com.rental.rental_system.tenant.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TenantRequest {
    // User fields
    @NotBlank private String name;
    @Email @NotBlank private String email;
    @NotBlank private String phone;
    private String password; // optional — defaults to tenant123

    // Tenant fields
    private String nationalId;
    private String emergencyContact;
    private String emergencyPhone;

    @NotNull private Long unitId;

    private LocalDate leaseStart;
    private LocalDate leaseEnd;
}