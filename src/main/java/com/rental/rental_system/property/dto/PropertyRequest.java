package com.rental.rental_system.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PropertyRequest {
    @NotBlank private String name;
    @NotBlank private String location;
    private String description;
    @NotNull private String status; // "ACTIVE" | "INACTIVE"
}