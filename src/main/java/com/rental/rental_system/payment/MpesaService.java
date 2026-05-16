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

    private final MpesaConfig    mpesaConfig;
    private final RestTemplate   restTemplate;
    private final ObjectMapper   objectMapper;

    // ── Get OAuth token ──────────────────────────────────
    public String getAccessToken() {
        String credentials = mpesaConfig.getConsumerKey()
                + ":" + mpesaConfig.getConsumerSecret();
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                mpesaConfig.getAuthUrl(), HttpMethod.GET, entity, String.class);

        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            return node.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get M-Pesa token: " + e.getMessage());
        }
    }

    // ── Initiate STK Push ────────────────────────────────
    public Map<String, String> initiateStkPush(
            String phoneNumber, String amount,
            String accountRef, String description) {

        String token     = getAccessToken();
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password  = Base64.getEncoder().encodeToString(
                (mpesaConfig.getShortcode()
                        + mpesaConfig.getPasskey()
                        + timestamp).getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = new HashMap<>();
        body.put("BusinessShortCode", mpesaConfig.getShortcode());
        body.put("Password",          password);
        body.put("Timestamp",         timestamp);
        body.put("TransactionType",   "CustomerPayBillOnline");
        body.put("Amount",            amount);
        body.put("PartyA",            phoneNumber);
        body.put("PartyB",            mpesaConfig.getShortcode());
        body.put("PhoneNumber",       phoneNumber);
        body.put("CallBackURL",       mpesaConfig.getCallbackUrl());
        body.put("AccountReference",  accountRef);
        body.put("TransactionDesc",   description);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    mpesaConfig.getStkUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);

            JsonNode node = objectMapper.readTree(response.getBody());
            Map<String, String> result = new HashMap<>();
            result.put("checkoutRequestId",  node.path("CheckoutRequestID").asText());
            result.put("merchantRequestId",  node.path("MerchantRequestID").asText());
            result.put("responseCode",       node.path("ResponseCode").asText());
            result.put("responseDescription",node.path("ResponseDescription").asText());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("STK Push failed: " + e.getMessage());
        }
    }
}