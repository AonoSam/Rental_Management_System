package com.rental.rental_system.payment;

import com.rental.rental_system.payment.dto.*;
import com.rental.rental_system.tenant.Tenant;
import com.rental.rental_system.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TenantRepository  tenantRepository;
    private final MpesaService      mpesaService;

    // ── Initiate STK Push ────────────────────────────────
    @Transactional
    public PaymentResponse initiatePayment(StkPushRequest req) {
        Tenant tenant = tenantRepository.findById(req.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Normalise phone: 07XXXXXXXX → 2547XXXXXXXX
        String phone = normalisePhone(req.getPhoneNumber());

        // Default payment month to current month
        String month = req.getPaymentMonth() != null
                ? req.getPaymentMonth()
                : YearMonth.now().toString(); // "2025-05"

        String accountRef = tenant.getUnit() != null
                ? tenant.getUnit().getHouseNumber()
                : "RENT";

        // Create pending payment record first
        Payment payment = Payment.builder()
                .tenant(tenant)
                .amount(req.getAmount())
                .phoneNumber(phone)
                .status(PaymentStatus.PENDING)
                .paymentMonth(month)
                .build();
        paymentRepository.save(payment);

        // Initiate STK Push
        try {
            Map<String, String> result = mpesaService.initiateStkPush(
                    phone,
                    req.getAmount().toPlainString(),
                    accountRef,
                    "Rent payment " + month
            );

            payment.setCheckoutRequestId(result.get("checkoutRequestId"));
            payment.setMerchantRequestId(result.get("merchantRequestId"));
            paymentRepository.save(payment);

            log.info("STK Push sent to {} for KES {}", phone, req.getAmount());
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            throw new RuntimeException("Failed to send STK Push: " + e.getMessage());
        }

        return toResponse(payment);
    }

    // ── Handle M-Pesa callback ───────────────────────────
    @Transactional
    public void handleCallback(Map<String, Object> callbackData) {
        try {
            log.info("M-Pesa callback received: {}", callbackData);

            // Navigate the nested callback JSON structure
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>)
                    ((Map<String, Object>) callbackData.get("Body"))
                            .get("stkCallback");

            String checkoutRequestId = (String) body.get("CheckoutRequestID");
            int    resultCode        = ((Number) body.get("ResultCode")).intValue();

            Payment payment = paymentRepository
                    .findByCheckoutRequestId(checkoutRequestId)
                    .orElse(null);

            if (payment == null) {
                log.warn("No payment found for checkoutRequestId: {}", checkoutRequestId);
                return;
            }

            if (resultCode == 0) {
                // Success — extract details from CallbackMetadata
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>)
                        ((Map<String, Object>) body.get("CallbackMetadata")).get("Item");

                String mpesaCode = null;
                for (Map<String, Object> item : items) {
                    if ("MpesaReceiptNumber".equals(item.get("Name"))) {
                        mpesaCode = (String) item.get("Value");
                    }
                }

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setMpesaCode(mpesaCode);
                payment.setPaidAt(LocalDateTime.now());
                log.info("Payment SUCCESS — M-Pesa code: {}", mpesaCode);
            } else {
                // Failed or cancelled
                String reason = (String) body.get("ResultDesc");
                payment.setStatus(resultCode == 1032
                        ? PaymentStatus.CANCELLED
                        : PaymentStatus.FAILED);
                payment.setFailureReason(reason);
                log.info("Payment FAILED — reason: {}", reason);
            }

            paymentRepository.save(payment);

        } catch (Exception e) {
            log.error("Error processing M-Pesa callback: {}", e.getMessage(), e);
        }
    }

    // ── Query payment status ─────────────────────────────
    public PaymentResponse getPaymentStatus(String checkoutRequestId) {
        Payment payment = paymentRepository
                .findByCheckoutRequestId(checkoutRequestId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return toResponse(payment);
    }

    // ── List payments ────────────────────────────────────
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<PaymentResponse> getTenantPayments(Long tenantId) {
        return paymentRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Dashboard stats ──────────────────────────────────
    public Map<String, Object> getPaymentStats() {
        String thisMonth = YearMonth.now().toString();
        BigDecimal monthlyRevenue = paymentRepository
                .sumSuccessfulPaymentsByMonth(thisMonth);

        return Map.of(
                "monthlyRevenue",      monthlyRevenue,
                "totalSuccessful",     paymentRepository.countSuccessfulPayments(),
                "pendingCount",        paymentRepository
                        .findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING).size()
        );
    }

    // ── Helpers ──────────────────────────────────────────
    private String normalisePhone(String phone) {
        phone = phone.trim().replaceAll("\\s+", "");
        if (phone.startsWith("0"))       return "254" + phone.substring(1);
        if (phone.startsWith("+254"))    return phone.substring(1);
        if (phone.startsWith("254"))     return phone;
        return phone;
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse.PaymentResponseBuilder b = PaymentResponse.builder()
                .id(p.getId())
                .tenantId(p.getTenant().getId())
                .tenantName(p.getTenant().getUser().getName())
                .amount(p.getAmount())
                .mpesaCode(p.getMpesaCode())
                .phoneNumber(p.getPhoneNumber())
                .status(p.getStatus().name())
                .paymentMonth(p.getPaymentMonth())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .paidAt(p.getPaidAt());

        if (p.getTenant().getUnit() != null) {
            b.unitNumber(p.getTenant().getUnit().getHouseNumber())
                    .propertyName(p.getTenant().getUnit().getProperty().getName());
        }

        return b.build();
    }
}