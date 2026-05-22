package com.rental.rental_system.payment;

public enum PaymentType {
    NORMAL,   // paid within window, exact amount
    LATE,     // paid outside window
    EXCESS,   // paid more than due
    CREDIT    // paid less due to credit from previous month
}