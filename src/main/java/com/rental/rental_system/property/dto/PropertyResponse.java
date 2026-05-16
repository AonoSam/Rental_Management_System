package com.rental.rental_system.property.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class PropertyResponse {
    private Long id;
    private String name;
    private String location;
    private String description;
    private int totalUnits;
    private String status;
    private long vacantUnits;
    private long occupiedUnits;
    private LocalDateTime createdAt;
}