package com.rental.rental_system.payment;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_start_day", nullable = false)
    private int paymentStartDay; // e.g. 1

    @Column(name = "payment_end_day", nullable = false)
    private int paymentEndDay;   // e.g. 5

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}