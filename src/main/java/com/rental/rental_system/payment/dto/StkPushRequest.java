package com.rental.rental_system.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class StkPushRequest {
    @NotNull  private Long tenantId;
    @NotNull  private BigDecimal amount;
    @NotBlank private String phoneNumber; // format: 2547XXXXXXXX
    private String paymentMonth;          // e.g. "2025-05", defaults to current month
    private String lateReason; // required only if paying late
}