package com.rental.rental_system.maintenance.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder
public class MaintenanceResponse {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String unitNumber;
    private String propertyName;
    private String issue;
    private String description;
    private String status;
    private String priority;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}