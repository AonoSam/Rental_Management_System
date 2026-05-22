package com.rental.rental_system.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentSettingsService {

    private final PaymentSettingsRepository settingsRepository;

    // ── Get current settings ─────────────────────────────
    public PaymentSettings getSettings() {
        return settingsRepository.findTopByOrderByIdDesc()
                .orElseGet(() -> {
                    // Default: 1st to 5th
                    PaymentSettings defaults = PaymentSettings.builder()
                            .paymentStartDay(1)
                            .paymentEndDay(5)
                            .build();
                    return settingsRepository.save(defaults);
                });
    }

    // ── Update settings ──────────────────────────────────
    @Transactional
    public PaymentSettings updateSettings(int startDay, int endDay) {
        if (startDay < 1 || startDay > 28)
            throw new RuntimeException("Start day must be between 1 and 28");
        if (endDay < startDay || endDay > 28)
            throw new RuntimeException("End day must be between start day and 28");

        PaymentSettings settings = settingsRepository
                .findTopByOrderByIdDesc()
                .orElse(new PaymentSettings());

        settings.setPaymentStartDay(startDay);
        settings.setPaymentEndDay(endDay);
        return settingsRepository.save(settings);
    }

    // ── Check if today is within payment window ──────────
    public boolean isWithinPaymentWindow() {
        PaymentSettings settings = getSettings();
        int today = LocalDate.now().getDayOfMonth();
        return today >= settings.getPaymentStartDay()
                && today <= settings.getPaymentEndDay();
    }

    // ── Get window info ──────────────────────────────────
    public Map<String, Object> getWindowInfo() {
        PaymentSettings s   = getSettings();
        boolean withinWindow = isWithinPaymentWindow();
        int today            = LocalDate.now().getDayOfMonth();
        int daysLeft         = s.getPaymentEndDay() - today;

        return Map.of(
                "startDay",      s.getPaymentStartDay(),
                "endDay",        s.getPaymentEndDay(),
                "withinWindow",  withinWindow,
                "today",         today,
                "daysLeft",      Math.max(0, daysLeft),
                "isLate",        !withinWindow
        );
    }
}