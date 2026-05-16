package com.rental.rental_system.payment.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class PaymentResponse {
    private Long id;
    private Long tenantId;
    private String tenantName;
    private String unitNumber;
    private String propertyName;
    private BigDecimal amount;
    private String mpesaCode;
    private String phoneNumber;
    private String status;
    private String paymentMonth;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}