package com.example.ecommerce.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * This is a TEST webhook receiver endpoint
 * Use this to simulate an external system receiving webhooks
 * <p>
 * In production, this would be a separate service/application
 */
@RestController
@RequestMapping("/test/webhook-receiver")
@Slf4j
public class WebhookReceiverController {

    /**
     * Test webhook endpoint that receives and logs webhook events
     * POST /test/webhook-receiver
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Event-Type", required = false) String eventType,
            @RequestHeader(value = "X-Delivery-ID", required = false) String deliveryId,
            @RequestHeader(value = "X-Attempt", required = false) String attempt) {

        log.info("========================================");
        log.info("📨 WEBHOOK RECEIVED");
        log.info("========================================");
        log.info("Event Type: {}", eventType);
        log.info("Delivery ID: {}", deliveryId);
        log.info("Attempt: {}", attempt);
        log.info("Signature: {}", signature != null ? signature.substring(0, 20) + "..." : "null");
        log.info("----------------------------------------");
        log.info("Payload: {}", payload);
        log.info("========================================");

        // Simulate processing delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // In a real system, you would:
        // 1. Verify the signature
        // 2. Process the event
        // 3. Update your database
        // 4. Send notifications
        // 5. Trigger other workflows

        return ResponseEntity.ok(Map.of(
                "status", "received",
                "eventType", eventType != null ? eventType : "unknown",
                "deliveryId", deliveryId != null ? deliveryId : "unknown",
                "message", "Webhook processed successfully"
        ));
    }

    /**
     * Endpoint that always fails (for testing retry logic)
     * POST /test/webhook-receiver/fail
     */
    @PostMapping("/fail")
    public ResponseEntity<Map<String, String>> failWebhook() {
        log.error("❌ WEBHOOK FAILED - Intentional failure for testing");
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Intentional failure for testing"));
    }

    /**
     * Endpoint that simulates timeout (for testing retry logic)
     * POST /test/webhook-receiver/timeout
     */
    @PostMapping("/timeout")
    public ResponseEntity<Map<String, String>> timeoutWebhook() {
        log.warn("⏱️ WEBHOOK TIMEOUT - Simulating slow response");

        try {
            Thread.sleep(30000); // 30 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return ResponseEntity.ok(Map.of("status", "timeout simulation"));
    }

    /**
     * Verify webhook signature
     * POST /test/webhook-receiver/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Webhook-Signature") String signature,
            @RequestParam String secret) {

        boolean valid = verifySignature(payload, signature, secret);

        log.info("Signature verification: {}", valid ? "✅ VALID" : "❌ INVALID");

        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "signature", signature,
                "payload", payload
        ));
    }

    private boolean verifySignature(String payload, String signature, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedSignature = Base64.getEncoder().encodeToString(hash);

            return computedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage());
            return false;
        }
    }
}