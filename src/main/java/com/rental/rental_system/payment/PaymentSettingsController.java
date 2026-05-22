package com.rental.rental_system.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment-settings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentSettingsController {

    private final PaymentSettingsService settingsService;

    // Get current settings + window status
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings() {
        return ResponseEntity.ok(settingsService.getWindowInfo());
    }

    // Admin updates payment window
    @PutMapping
    public ResponseEntity<PaymentSettings> updateSettings(
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(settingsService.updateSettings(
                body.get("startDay"), body.get("endDay")));
    }
}