package com.rental.rental_system.property.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class UnitResponse {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private String houseNumber;
    private String category;
    private BigDecimal rentAmount;
    private int floor;
    private String status;
    private LocalDateTime createdAt;
}