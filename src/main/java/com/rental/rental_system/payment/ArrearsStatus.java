package com.rental.rental_system.payment;

public enum ArrearsStatus {
    PARTIAL,   // paid less than due
    CLEARED,   // paid exactly or more than due
    EXCESS     // overpaid (credit)
}