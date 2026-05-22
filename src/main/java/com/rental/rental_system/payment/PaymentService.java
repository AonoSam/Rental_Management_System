package com.rental.rental_system.payment;

import com.rental.rental_system.notification.NotificationService;
import com.rental.rental_system.payment.dto.*;
import com.rental.rental_system.tenant.Tenant;
import com.rental.rental_system.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rental.rental_system.tenant.TenantRepository;
import com.rental.rental_system.payment.ArrearsRepository;

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
    private final TenantRepository tenantRepository;
    private final MpesaService mpesaService;
    private final NotificationService notificationService;
    private final PaymentSettingsService  paymentSettingsService;
    private final ArrearsRepository arrearsRepository;


    // ── Initiate STK Push ───────────────────────────────

    @Transactional
    public PaymentResponse initiatePayment(StkPushRequest req) {
        Tenant tenant = tenantRepository.findById(req.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        String phone = normalisePhone(req.getPhoneNumber());
        String month = req.getPaymentMonth() != null
                ? req.getPaymentMonth()
                : java.time.YearMonth.now().toString();

        // ── Duplicate payment check ──────────────────────────
        boolean alreadyPaid = paymentRepository
                .existsByTenantIdAndPaymentMonthAndStatus(
                        tenant.getId(), month, PaymentStatus.SUCCESS);
        if (alreadyPaid)
            throw new RuntimeException(
                    "Rent for " + month + " has already been fully paid");

        // ── Payment window check ─────────────────────────────
        boolean withinWindow = paymentSettingsService.isWithinPaymentWindow();
        boolean isLate       = !withinWindow;

        if (isLate && (req.getLateReason() == null
                || req.getLateReason().trim().isEmpty()))
            throw new RuntimeException(
                    "Payment window is closed. Please provide a reason.");

        // ── Calculate amounts ────────────────────────────────
        java.math.BigDecimal rentAmount = tenant.getUnit() != null
                ? tenant.getUnit().getRentAmount()
                : req.getAmount();

        // Previous arrears
        java.math.BigDecimal carriedArrears =
                tenant.getArrearsBalance() != null
                        ? tenant.getArrearsBalance()
                        : java.math.BigDecimal.ZERO;

        // Credit balance from overpayment
        java.math.BigDecimal creditBalance =
                tenant.getCreditBalance() != null
                        ? tenant.getCreditBalance()
                        : java.math.BigDecimal.ZERO;

        // Total due = this month + arrears - credit
        java.math.BigDecimal totalDue = rentAmount
                .add(carriedArrears)
                .subtract(creditBalance)
                .max(java.math.BigDecimal.ZERO);

        java.math.BigDecimal amountPaying = req.getAmount();

        // ── Calculate result ─────────────────────────────────
        java.math.BigDecimal balanceRemaining = java.math.BigDecimal.ZERO;
        java.math.BigDecimal excessAmount     = java.math.BigDecimal.ZERO;
        java.math.BigDecimal creditUsed       = creditBalance;
        ArrearsStatus        arrearsStatus;
        PaymentType          paymentType;

        int cmp = amountPaying.compareTo(totalDue);

        if (cmp < 0) {
            // Underpaid — new arrears
            balanceRemaining = totalDue.subtract(amountPaying);
            arrearsStatus    = ArrearsStatus.PARTIAL;
            paymentType      = isLate ? PaymentType.LATE : PaymentType.NORMAL;
        } else if (cmp == 0) {
            // Exact payment
            balanceRemaining = java.math.BigDecimal.ZERO;
            arrearsStatus    = ArrearsStatus.CLEARED;
            paymentType      = isLate ? PaymentType.LATE : PaymentType.NORMAL;
        } else {
            // Overpaid — excess becomes credit
            excessAmount     = amountPaying.subtract(totalDue);
            balanceRemaining = java.math.BigDecimal.ZERO;
            arrearsStatus    = ArrearsStatus.EXCESS;
            paymentType      = PaymentType.EXCESS;
        }

        // ── Create arrears record ────────────────────────────
        ArrearsRecord arrearsRecord = ArrearsRecord.builder()
                .tenant(tenant)
                .paymentMonth(month)
                .rentAmount(rentAmount)
                .amountPaid(amountPaying)
                .arrearsAmount(balanceRemaining)
                .carriedArrears(carriedArrears)
                .totalDue(totalDue)
                .balanceRemaining(balanceRemaining)
                .status(arrearsStatus)
                .build();
        arrearsRepository.save(arrearsRecord);

        // ── Update tenant balances ───────────────────────────
        tenant.setArrearsBalance(balanceRemaining);
        tenant.setCreditBalance(excessAmount);
        tenantRepository.save(tenant);

        // ── Create payment ───────────────────────────────────
        String accountRef = tenant.getUnit() != null
                ? tenant.getUnit().getHouseNumber() : "RENT";

        Payment payment = Payment.builder()
                .tenant(tenant)
                .amount(amountPaying)
                .expectedAmount(totalDue)
                .excessAmount(excessAmount)
                .creditUsed(creditUsed)
                .phoneNumber(phone)
                .status(PaymentStatus.PENDING)
                .paymentMonth(month)
                .paymentType(paymentType)
                .lateReason(isLate ? req.getLateReason() : null)
                .build();
        paymentRepository.save(payment);

        // ── STK Push ─────────────────────────────────────────
        try {
            Map<String, String> result = mpesaService.initiateStkPush(
                    phone, amountPaying.toPlainString(),
                    accountRef, "Rent " + month);
            payment.setCheckoutRequestId(result.get("checkoutRequestId"));
            payment.setMerchantRequestId(result.get("merchantRequestId"));
            paymentRepository.save(payment);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());
            paymentRepository.save(payment);
            // Reverse arrears update on failure
            tenant.setArrearsBalance(carriedArrears);
            tenant.setCreditBalance(creditBalance);
            tenantRepository.save(tenant);
            throw new RuntimeException("STK Push failed: " + e.getMessage());
        }

        return toResponse(payment);
    }

    // ── Handle M-Pesa callback ───────────────────────────
    @Transactional
    public void handleCallback(Map<String, Object> callbackData) {

        try {

            log.info("M-Pesa callback received: {}", callbackData);

            // Navigate callback JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>)
                    ((Map<String, Object>) callbackData.get("Body"))
                            .get("stkCallback");

            String checkoutRequestId =
                    (String) body.get("CheckoutRequestID");

            int resultCode =
                    ((Number) body.get("ResultCode")).intValue();

            Payment payment = paymentRepository
                    .findByCheckoutRequestId(checkoutRequestId)
                    .orElse(null);

            if (payment == null) {

                log.warn("No payment found for checkoutRequestId: {}",
                        checkoutRequestId);

                return;
            }

            // SUCCESS
            if (resultCode == 0) {

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items =
                        (List<Map<String, Object>>)
                                ((Map<String, Object>) body.get("CallbackMetadata"))
                                        .get("Item");

                String mpesaCode = null;

                for (Map<String, Object> item : items) {

                    if ("MpesaReceiptNumber".equals(item.get("Name"))) {
                        mpesaCode = (String) item.get("Value");
                    }
                }

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setMpesaCode(mpesaCode);
                payment.setPaidAt(LocalDateTime.now());

                // Send success notification
                notificationService.paymentReceived(
                        payment.getTenant().getUser(),
                        payment.getAmount().toPlainString(),
                        mpesaCode,
                        payment.getPaymentMonth()
                );

                log.info("Payment SUCCESS — M-Pesa code: {}", mpesaCode);

            } else {

                // FAILED OR CANCELLED
                String reason = (String) body.get("ResultDesc");

                payment.setStatus(
                        resultCode == 1032
                                ? PaymentStatus.CANCELLED
                                : PaymentStatus.FAILED
                );

                payment.setFailureReason(reason);

                // Send failed notification
                notificationService.paymentFailed(
                        payment.getTenant().getUser(),
                        payment.getAmount().toPlainString(),
                        reason
                );

                log.info("Payment FAILED — reason: {}", reason);
            }

            paymentRepository.save(payment);

        } catch (Exception e) {

            log.error("Error processing M-Pesa callback: {}",
                    e.getMessage(), e);
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
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getTenantPayments(Long tenantId) {

        return paymentRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ── Dashboard stats ──────────────────────────────────
    public Map<String, Object> getPaymentStats() {

        String thisMonth = YearMonth.now().toString();

        BigDecimal monthlyRevenue = paymentRepository
                .sumSuccessfulPaymentsByMonth(thisMonth);

        return Map.of(
                "monthlyRevenue", monthlyRevenue,
                "totalSuccessful", paymentRepository.countSuccessfulPayments(),
                "pendingCount",
                paymentRepository
                        .findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING)
                        .size()
        );
    }

    // ── Helpers ──────────────────────────────────────────
    private String normalisePhone(String phone) {

        phone = phone.trim().replaceAll("\\s+", "");

        if (phone.startsWith("0")) {
            return "254" + phone.substring(1);
        }

        if (phone.startsWith("+254")) {
            return phone.substring(1);
        }

        if (phone.startsWith("254")) {
            return phone;
        }

        return phone;
    }

    private PaymentResponse toResponse(Payment p) {
        PaymentResponse.PaymentResponseBuilder b = PaymentResponse.builder()
                .id(p.getId())
                .tenantId(p.getTenant().getId())
                .tenantName(p.getTenant().getUser().getName())
                .amount(p.getAmount())
                .expectedAmount(p.getExpectedAmount())
                .excessAmount(p.getExcessAmount())
                .creditUsed(p.getCreditUsed())
                .mpesaCode(p.getMpesaCode())
                .phoneNumber(p.getPhoneNumber())
                .status(p.getStatus().name())
                .paymentType(p.getPaymentType() != null
                        ? p.getPaymentType().name() : null)
                .lateReason(p.getLateReason())
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