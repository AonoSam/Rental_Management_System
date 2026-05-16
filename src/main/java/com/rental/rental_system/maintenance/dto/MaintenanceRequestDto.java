package com.rental.rental_system.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MaintenanceRequestDto {
    @NotBlank private String issue;
    private String description;
    private String priority; // LOW, MEDIUM, HIGH, URGENT
}