package com.rental.rental_system.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpesaService {

    private final MpesaConfig mpesaConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ─────────────────────────────────────────────
    // 1. ACCESS TOKEN
    // ─────────────────────────────────────────────
    public String getAccessToken() {

        try {
            String credentials = mpesaConfig.getConsumerKey()
                    + ":" + mpesaConfig.getConsumerSecret();

            String encoded = Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Basic " + encoded);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    mpesaConfig.getAuthUrl(),
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // LOG FULL RESPONSE
            log.info("M-PESA RESPONSE STATUS: {}", response.getStatusCode());
            log.info("M-PESA RESPONSE BODY: {}", response.getBody());

            JsonNode node = objectMapper.readTree(response.getBody());

            String token = node.path("access_token").asText();

            if (token == null || token.isEmpty()) {
                throw new RuntimeException("Empty access token from M-Pesa");
            }

            return token;

        } catch (Exception e) {
            log.error("TOKEN ERROR: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get M-Pesa token");
        }
    }

    // ─────────────────────────────────────────────
    // 2. STK PUSH
    // ─────────────────────────────────────────────
    public Map<String, String> initiateStkPush(
            String phoneNumber,
            String amount,
            String accountRef,
            String description
    ) {

        try {
            String token = getAccessToken();

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            String passwordRaw = mpesaConfig.getShortcode()
                    + mpesaConfig.getPasskey()
                    + timestamp;

            String password = Base64.getEncoder()
                    .encodeToString(passwordRaw.getBytes(StandardCharsets.UTF_8));

            Map<String, Object> body = new HashMap<>();
            body.put("BusinessShortCode", mpesaConfig.getShortcode());
            body.put("Password", password);
            body.put("Timestamp", timestamp);
            body.put("TransactionType", "CustomerPayBillOnline");
            body.put("Amount", amount);
            body.put("PartyA", phoneNumber);
            body.put("PartyB", mpesaConfig.getShortcode());
            body.put("PhoneNumber", phoneNumber);
            body.put("CallBackURL", mpesaConfig.getCallbackUrl());
            body.put("AccountReference", accountRef);
            body.put("TransactionDesc", description);

            log.info("STK REQUEST: {}", body);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> response = restTemplate.exchange(
                    mpesaConfig.getStkUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            log.info("STK RESPONSE: {}", response.getBody());

            log.info("STK PUSH RESPONSE STATUS: {}", response.getStatusCode());
            log.info("STK PUSH RESPONSE BODY: {}", response.getBody());

            JsonNode node = objectMapper.readTree(response.getBody());

            String responseCode = node.path("ResponseCode").asText();

            // 🔥 IMPORTANT CHECK
            if (!"0".equals(responseCode)) {
                throw new RuntimeException("STK rejected: " + response.getBody());
            }

            Map<String, String> result = new HashMap<>();
            result.put("checkoutRequestId", node.path("CheckoutRequestID").asText());
            result.put("merchantRequestId", node.path("MerchantRequestID").asText());
            result.put("responseCode", responseCode);
            result.put("responseDescription", node.path("ResponseDescription").asText());

            return result;

        } catch (Exception e) {
            log.error("FULL STK ERROR", e);
            throw new RuntimeException("STK Push failed: " + e.getMessage());
        }
    }
}