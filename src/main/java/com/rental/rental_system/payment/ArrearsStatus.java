package com.rental.rental_system.payment;

public enum ArrearsStatus {
    PARTIAL,  // paid less than due
    CLEARED,  // fully paid
    OVERDUE,  // month ended unpaid
    EXCESS    // overpaid
}