package com.rental.rental_system.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UnitRequest {
    @NotBlank private String houseNumber;
    @NotNull  private String category;    // "BEDSITTER", "ONE_BEDROOM" etc.
    @NotNull  private BigDecimal rentAmount;
    private int floor;
    @NotNull  private String status;      // "VACANT" etc.
}