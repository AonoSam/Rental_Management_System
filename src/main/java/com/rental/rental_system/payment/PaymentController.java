package com.rental.rental_system.payment;

import com.rental.rental_system.payment.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // Initiate STK Push
    @PostMapping("/mpesa/stk-push")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody StkPushRequest req) {
        return ResponseEntity.ok(paymentService.initiatePayment(req));
    }

    // M-Pesa callback — must be publicly accessible via ngrok
    @PostMapping("/mpesa/callback")
    public ResponseEntity<String> callback(@RequestBody Map<String, Object> body) {
        paymentService.handleCallback(body);

        return ResponseEntity.ok("OK");
    }

    // Poll payment status (frontend polls this after STK push)
    @GetMapping("/status/{checkoutRequestId}")
    public ResponseEntity<PaymentResponse> getStatus(
            @PathVariable String checkoutRequestId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(checkoutRequestId));
    }

    // All payments (admin)
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAll() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    // Payments for a specific tenant
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<PaymentResponse>> getTenantPayments(
            @PathVariable Long tenantId) {
        return ResponseEntity.ok(paymentService.getTenantPayments(tenantId));
    }

    // Payment stats for dashboard
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(paymentService.getPaymentStats());
    }
}