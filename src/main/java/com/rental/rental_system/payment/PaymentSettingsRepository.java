package com.rental.rental_system.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentSettingsRepository
        extends JpaRepository<PaymentSettings, Long> {
    Optional<PaymentSettings> findTopByOrderByIdDesc();
}