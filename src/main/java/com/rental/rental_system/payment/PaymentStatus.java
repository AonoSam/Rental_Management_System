package com.rental.rental_system.payment;

public enum PaymentStatus {
    PENDING,   // STK push sent, waiting for PIN
    PAID,      // Full month cleared
    PARTIAL,   // Some money paid, balance remains
    FAILED,    // Rejected by M-Pesa or error
    CANCELLED, // Tenant cancelled (did not enter PIN)
    OVERDUE    // Month ended without full payment
}