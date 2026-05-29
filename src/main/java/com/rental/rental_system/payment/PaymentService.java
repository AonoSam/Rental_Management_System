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

        // ── Payment window check ─────────────────────────────
        boolean withinWindow = paymentSettingsService.isWithinPaymentWindow();
        boolean isLate = !withinWindow;

        if (isLate && (req.getLateReason() == null
                || req.getLateReason().trim().isEmpty())) {

            throw new RuntimeException(
                    "Payment window is closed. Please provide a reason.");
        }

        // ── Calculate amounts ────────────────────────────────
        java.math.BigDecimal rentAmount = tenant.getUnit() != null
                ? tenant.getUnit().getRentAmount()
                : req.getAmount();

        String currentMonth = java.time.YearMonth.now().toString();
        String payingMonth = month;

        // Get carried arrears — only from same/past months
        java.math.BigDecimal carriedArrears = java.math.BigDecimal.ZERO;

        // Check existing arrears record for this month
        java.util.Optional<ArrearsRecord> existingRecord =
                arrearsRepository.findByTenantIdAndPaymentMonth(
                        tenant.getId(), payingMonth);

        if (existingRecord.isPresent()) {

            // Tenant topping up remaining balance
            carriedArrears = existingRecord.get().getBalanceRemaining();

            // Only remaining balance should be paid
            rentAmount = carriedArrears;

        } else {

            // Only add previous arrears
            carriedArrears = tenant.getArrearsBalance() != null
                    ? tenant.getArrearsBalance()
                    : java.math.BigDecimal.ZERO;
        }

        // Credit balance
        java.math.BigDecimal creditBalance = tenant.getCreditBalance() != null
                ? tenant.getCreditBalance()
                : java.math.BigDecimal.ZERO;

        // Total due
        java.math.BigDecimal totalDue = rentAmount
                .add(existingRecord.isPresent()
                        ? java.math.BigDecimal.ZERO
                        : carriedArrears)
                .subtract(creditBalance)
                .max(java.math.BigDecimal.ZERO);

        java.math.BigDecimal amountPaying = req.getAmount();

        // ── Determine payment result ─────────────────────────
        java.math.BigDecimal balanceRemaining = java.math.BigDecimal.ZERO;
        java.math.BigDecimal excessAmount = java.math.BigDecimal.ZERO;
        java.math.BigDecimal creditUsed = creditBalance;

        ArrearsStatus arrearsStatus;
        PaymentType paymentType;

        int cmp = amountPaying.compareTo(totalDue);

        if (cmp < 0) {

            // Partial payment
            balanceRemaining = totalDue.subtract(amountPaying);

            arrearsStatus = ArrearsStatus.PARTIAL;

            paymentType = isLate
                    ? PaymentType.LATE
                    : PaymentType.NORMAL;

        } else if (cmp == 0) {

            // Fully paid
            balanceRemaining = java.math.BigDecimal.ZERO;

            arrearsStatus = ArrearsStatus.CLEARED;

            paymentType = isLate
                    ? PaymentType.LATE
                    : PaymentType.NORMAL;

        } else {

            // Overpayment
            excessAmount = amountPaying.subtract(totalDue);

            balanceRemaining = java.math.BigDecimal.ZERO;

            arrearsStatus = ArrearsStatus.EXCESS;

            paymentType = PaymentType.EXCESS;
        }

        // ── Create payment FIRST (pending) ───────────────────
        String accountRef = tenant.getUnit() != null
                ? tenant.getUnit().getHouseNumber()
                : "RENT";

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
                    phone,
                    amountPaying.toPlainString(),
                    accountRef,
                    "Rent " + month
            );

            payment.setCheckoutRequestId(result.get("checkoutRequestId"));
            payment.setMerchantRequestId(result.get("merchantRequestId"));

            paymentRepository.save(payment);

        } catch (Exception e) {

            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());

            paymentRepository.save(payment);

            throw new RuntimeException("STK Push failed: " + e.getMessage());
        }

        // ── IMPORTANT ────────────────────────────────────────
        // DO NOT update tenant balances here
        // DO NOT create arrears records here
        // ONLY do that after successful M-PESA callback

        return toResponse(payment);
    }

    // ── Handle M-Pesa callback ───────────────────────────
    // ── Handle M-Pesa callback ───────────────────────────
    @Transactional
    public void handleCallback(Map<String, Object> callbackData) {

        try {

            log.info("M-Pesa callback received: {}", callbackData);

            @SuppressWarnings("unchecked")
            Map<String, Object> stkCallback =
                    (Map<String, Object>)
                            ((Map<String, Object>) callbackData.get("Body"))
                                    .get("stkCallback");

            String checkoutRequestId =
                    (String) stkCallback.get("CheckoutRequestID");

            int resultCode =
                    ((Number) stkCallback.get("ResultCode")).intValue();

            String resultDesc =
                    (String) stkCallback.get("ResultDesc");

            Payment payment = paymentRepository
                    .findByCheckoutRequestId(checkoutRequestId)
                    .orElse(null);

            if (payment == null) {
                log.warn("Payment not found for checkoutRequestId={}",
                        checkoutRequestId);
                return;
            }

            Tenant tenant = payment.getTenant();

            // Prevent double processing
            if (payment.getStatus() == PaymentStatus.PAID
                    || payment.getStatus() == PaymentStatus.PARTIAL
                    || payment.getStatus() == PaymentStatus.CANCELLED
                    || payment.getStatus() == PaymentStatus.FAILED) {

                log.info("Payment already processed: {}", payment.getId());
                return;
            }

            // ───────────────── SUCCESS ─────────────────
            if (resultCode == 0) {

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items =
                        (List<Map<String, Object>>)
                                ((Map<String, Object>) stkCallback.get("CallbackMetadata"))
                                        .get("Item");

                String mpesaCode = null;

                for (Map<String, Object> item : items) {

                    if ("MpesaReceiptNumber".equals(item.get("Name"))) {

                        mpesaCode = String.valueOf(item.get("Value"));
                    }
                }

                BigDecimal amountPaid =
                        payment.getAmount() != null
                                ? payment.getAmount()
                                : BigDecimal.ZERO;

                BigDecimal totalDue =
                        payment.getExpectedAmount() != null
                                ? payment.getExpectedAmount()
                                : amountPaid;

                BigDecimal balanceRemaining =
                        totalDue.subtract(amountPaid);

                if (balanceRemaining.compareTo(BigDecimal.ZERO) < 0) {

                    balanceRemaining = BigDecimal.ZERO;
                }

                // ── Payment status ─────────────────────
                if (balanceRemaining.compareTo(BigDecimal.ZERO) == 0) {

                    payment.setStatus(PaymentStatus.PAID);

                } else {

                    payment.setStatus(PaymentStatus.PARTIAL);
                }

                payment.setMpesaCode(mpesaCode);
                payment.setPaidAt(LocalDateTime.now());

                // ── Existing arrears record ───────────
                ArrearsRecord arrearsRecord =
                        arrearsRepository
                                .findByTenantIdAndPaymentMonth(
                                        tenant.getId(),
                                        payment.getPaymentMonth())
                                .orElse(null);

                if (arrearsRecord == null) {

                    arrearsRecord = ArrearsRecord.builder()
                            .tenant(tenant)
                            .paymentMonth(payment.getPaymentMonth())
                            .rentAmount(
                                    tenant.getUnit() != null
                                            ? tenant.getUnit().getRentAmount()
                                            : amountPaid)
                            .carriedArrears(
                                    tenant.getArrearsBalance() != null
                                            ? tenant.getArrearsBalance()
                                            : BigDecimal.ZERO)
                            .totalDue(totalDue)
                            .amountPaid(amountPaid)
                            .balanceRemaining(balanceRemaining)
                            .arrearsAmount(balanceRemaining)
                            .status(
                                    balanceRemaining.compareTo(BigDecimal.ZERO) == 0
                                            ? ArrearsStatus.CLEARED
                                            : ArrearsStatus.PARTIAL)
                            .build();

                } else {

                    BigDecimal newAmountPaid =
                            arrearsRecord.getAmountPaid()
                                    .add(amountPaid);

                    BigDecimal newBalance =
                            arrearsRecord.getTotalDue()
                                    .subtract(newAmountPaid);

                    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {

                        newBalance = BigDecimal.ZERO;
                    }

                    arrearsRecord.setAmountPaid(newAmountPaid);
                    arrearsRecord.setBalanceRemaining(newBalance);
                    arrearsRecord.setArrearsAmount(newBalance);

                    arrearsRecord.setStatus(
                            newBalance.compareTo(BigDecimal.ZERO) == 0
                                    ? ArrearsStatus.CLEARED
                                    : ArrearsStatus.PARTIAL);

                    balanceRemaining = newBalance;
                }

                arrearsRepository.save(arrearsRecord);

                // ── Update tenant balances ────────────
                tenant.setArrearsBalance(balanceRemaining);

                // Only clear credit when fully paid
                if (balanceRemaining.compareTo(BigDecimal.ZERO) == 0) {

                    tenant.setCreditBalance(BigDecimal.ZERO);
                }

                tenantRepository.save(tenant);

                notificationService.paymentReceived(
                        tenant.getUser(),
                        amountPaid.toPlainString(),
                        mpesaCode,
                        payment.getPaymentMonth());

                log.info("Payment successful: {}", payment.getId());
            }

            // ───────────────── CANCELLED ─────────────
            else if (resultCode == 1032) {

                payment.setStatus(PaymentStatus.CANCELLED);

                payment.setFailureReason(
                        "Transaction cancelled by tenant");

                notificationService.paymentFailed(
                        tenant.getUser(),
                        payment.getAmount().toPlainString(),
                        "Transaction cancelled");

                log.info("Payment cancelled: {}", payment.getId());
            }

            // ───────────────── FAILED ────────────────
            else {

                payment.setStatus(PaymentStatus.FAILED);

                payment.setFailureReason(resultDesc);

                notificationService.paymentFailed(
                        tenant.getUser(),
                        payment.getAmount().toPlainString(),
                        resultDesc);

                log.info("Payment failed: {}", resultDesc);
            }

            paymentRepository.save(payment);

        } catch (Exception e) {

            log.error("Callback processing error", e);

            throw new RuntimeException(
                    "Failed to process callback: " + e.getMessage());
        }
    }

    // ── Query payment status ─────────────────────────────
    public PaymentResponse getPaymentStatus(String checkoutRequestId) {

        Payment payment = paymentRepository
                .findByCheckoutRequestId(checkoutRequestId)
                .orElse(null);

        if (payment == null) {
            return null;
        }

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