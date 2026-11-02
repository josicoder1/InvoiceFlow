package com.invoiceflow.infrastructure.payment;
 
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.domain.port.out.PaymentPort;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
 
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
 
@Component
public class ChapaPaymentAdapter implements PaymentPort {
 
    private final ChapaProperties chapaProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
 
    public ChapaPaymentAdapter(ChapaProperties chapaProperties, ObjectMapper objectMapper) {
        this.chapaProperties = chapaProperties;
        this.objectMapper = objectMapper;
    }
 
    @Override
    public CheckoutSession createCheckoutSession(CheckoutRequest request) {
        String secretKey = request.chapaMerchantKey();
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = chapaProperties.secretKey();
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Chapa secret key is not configured");
        }
 
        // Chapa uses tx_ref as a unique reference. Let's generate one or use a combination of invoice number and UUID
        String txRef = "tx-" + request.invoiceId() + "-" + UUID.randomUUID().toString().substring(0, 8);
 
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("amount", request.amount().toString());
            body.put("currency", request.currency().toUpperCase());
            body.put("email", request.clientEmail());
            body.put("first_name", "Valued");
            body.put("last_name", "Customer");
            body.put("tx_ref", txRef);
            body.put("callback_url", request.successUrl());
            body.put("return_url", request.successUrl()); // Chapa sometimes uses return_url
            
            Map<String, String> customization = new HashMap<>();
            customization.put("title", "Invoice Payment");
            customization.put("description", "Payment for Invoice " + request.invoiceNumber());
            body.put("customization", customization);
 
            Map<String, String> meta = new HashMap<>();
            meta.put("invoiceId", request.invoiceId());
            body.put("meta", meta);
 
            String requestBody = objectMapper.writeValueAsString(body);
 
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.chapa.co/v1/transaction/initialize"))
                    .header("Authorization", "Bearer " + secretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
 
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new RuntimeException("Chapa API returned error status: " + httpResponse.statusCode() + " Body: " + httpResponse.body());
            }
 
            JsonNode json = objectMapper.readTree(httpResponse.body());
            String checkoutUrl = json.path("data").path("checkout_url").asText(null);
            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new IllegalStateException("Chapa response did not contain checkout_url");
            }
 
            // Return checkout session
            return new CheckoutSession(txRef, checkoutUrl, txRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Chapa payment transaction: " + e.getMessage(), e);
        }
    }
 
    @Override
    public WebhookEvent parseWebhook(String payload, String signatureHeader) {
        String webhookSecret = chapaProperties.webhookSecret();
        if (webhookSecret != null && !webhookSecret.isBlank() && signatureHeader != null) {
            verifySignature(payload, signatureHeader, webhookSecret);
        }
 
        try {
            JsonNode json = objectMapper.readTree(payload);
            // In Chapa, webhook payload has event at root or under standard fields
            // Payload example:
            // {
            //   "event": "charge.success",
            //   "data": {
            //      "tx_ref": "...",
            //      "status": "success",
            //      "amount": 100,
            //      ...
            //   }
            // }
            // or sometimes flatter depending on version. Let's handle both nested and flat structure safely.
            String eventType = json.path("event").asText("charge.success");
            
            JsonNode dataNode = json.has("data") ? json.get("data") : json;
            String txRef = dataNode.path("tx_ref").asText(null);
            String status = dataNode.path("status").asText(null);
            
            // Extract invoice ID from metadata or parse it from txRef (tx-{invoiceId}-{rand})
            String invoiceId = null;
            if (dataNode.has("meta") && dataNode.get("meta").has("invoiceId")) {
                invoiceId = dataNode.get("meta").path("invoiceId").asText(null);
            }
            if ((invoiceId == null || invoiceId.isBlank()) && txRef != null && txRef.startsWith("tx-")) {
                String[] parts = txRef.split("-");
                if (parts.length >= 2) {
                    invoiceId = parts[1];
                }
            }
 
            if ("success".equalsIgnoreCase(status) || "success".equalsIgnoreCase(json.path("status").asText(null))) {
                return new WebhookEvent(eventType, invoiceId, txRef, txRef);
            }
            return new WebhookEvent("failed", invoiceId, txRef, txRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Chapa webhook: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyTransaction(String txRef, String chapaMerchantKey) {
        String secretKey = chapaMerchantKey;
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = chapaProperties.secretKey();
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Chapa secret key is not configured");
        }

        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.chapa.co/v1/transaction/verify/" + txRef))
                    .header("Authorization", "Bearer " + secretKey)
                    .GET()
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                return false;
            }

            JsonNode json = objectMapper.readTree(httpResponse.body());
            String status = json.path("status").asText(null);
            String dataStatus = json.path("data").path("status").asText(null);
            return "success".equalsIgnoreCase(status) || "success".equalsIgnoreCase(dataStatus);
        } catch (Exception e) {
            return false;
        }
    }
 
    private void verifySignature(String payload, String signatureHeader, String secret) {
        try {
            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256HMAC.init(secretKey);
            byte[] hashBytes = sha256HMAC.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String computed = hexString.toString();
            
            if (!MessageDigest.isEqual(computed.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid Chapa webhook signature");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to verify Chapa signature: " + e.getMessage(), e);
        }
    }
}
