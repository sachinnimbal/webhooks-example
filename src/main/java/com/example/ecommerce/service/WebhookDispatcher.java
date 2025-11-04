package com.example.ecommerce.service;

import com.example.ecommerce.model.*;
import com.example.ecommerce.respository.WebhookDeliveryRepository;
import com.example.ecommerce.respository.WebhookSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcher {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Main method to dispatch webhook events
     */
    @Async
    public void dispatchProductEvent(String eventType, Product product, ChangeDetails changes) {
        log.info("Dispatching webhook event: {} for product: {}", eventType, product.getSku());

        // Find all active subscriptions for this event type
        List<WebhookSubscription> subscriptions = subscriptionRepository
                .findByActiveAndSubscribedEventsContaining(true, eventType);

        if (subscriptions.isEmpty()) {
            log.info("No active subscriptions found for event: {}", eventType);
            return;
        }

        // Create webhook payload
        ProductWebhookPayload payload = buildPayload(eventType, product, changes);

        // Deliver to each subscription
        for (WebhookSubscription subscription : subscriptions) {
            deliverWebhook(subscription, payload);
        }
    }

    /**
     * Build the webhook payload
     */
    private ProductWebhookPayload buildPayload(String eventType, Product product, ChangeDetails changes) {
        return ProductWebhookPayload.builder()
                .eventType(eventType)
                .eventId(System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .data(ProductData.builder()
                        .id(product.getId())
                        .sku(product.getSku())
                        .name(product.getName())
                        .price(product.getPrice())
                        .stockQuantity(product.getStockQuantity())
                        .status(product.getStatus())
                        .category(product.getCategory())
                        .build())
                .changes(changes)
                .build();
    }

    /**
     * Deliver webhook to a subscription endpoint
     */
    @Transactional
    public void deliverWebhook(WebhookSubscription subscription, ProductWebhookPayload payload) {

        WebhookDelivery delivery = null;

        try {
            // Create delivery record
            String payloadJson = objectMapper.writeValueAsString(payload);

            delivery = WebhookDelivery.builder()
                    .webhookUrl(subscription.getWebhookUrl())
                    .eventType(payload.getEventType())
                    .payload(payloadJson)
                    .status(DeliveryStatus.PENDING)
                    .attempts(0)
                    .maxAttempts(3)
                    .createdAt(LocalDateTime.now())
                    .build();

            delivery = deliveryRepository.save(delivery);

            // Attempt delivery
            attemptDelivery(delivery, subscription.getSecret());

        } catch (Exception e) {
            log.error("Failed to deliver webhook: {}", e.getMessage(), e);

            if (delivery != null) {
                delivery.setStatus(DeliveryStatus.FAILED);
                delivery.setErrorMessage(e.getMessage());
                deliveryRepository.save(delivery);
            }
        }
    }

    /**
     * Attempt to deliver webhook with signature
     */
    private void attemptDelivery(WebhookDelivery delivery, String secret) {
        try {
            delivery.setAttempts(delivery.getAttempts() + 1);

            // Generate signature
            String signature = generateSignature(delivery.getPayload(), secret);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", signature);
            headers.set("X-Event-Type", delivery.getEventType());
            headers.set("X-Delivery-ID", delivery.getId().toString());
            headers.set("X-Attempt", delivery.getAttempts().toString());

            HttpEntity<String> request = new HttpEntity<>(delivery.getPayload(), headers);

            log.info("Delivering webhook to: {} (attempt {})",
                    delivery.getWebhookUrl(), delivery.getAttempts());

            // Send webhook
            ResponseEntity<String> response = restTemplate.postForEntity(
                    delivery.getWebhookUrl(),
                    request,
                    String.class
            );

            // Update delivery status
            if (response.getStatusCode().is2xxSuccessful()) {
                delivery.setStatus(DeliveryStatus.DELIVERED);
                delivery.setResponseCode(response.getStatusCode().toString());
                delivery.setDeliveredAt(LocalDateTime.now());

                log.info("Webhook delivered successfully: {}", delivery.getId());
            } else {
                handleFailedDelivery(delivery, "Non-200 response: " + response.getStatusCode());
            }

            deliveryRepository.save(delivery);

        } catch (Exception e) {
            log.error("Delivery attempt failed: {}", e.getMessage());
            handleFailedDelivery(delivery, e.getMessage());
            deliveryRepository.save(delivery);
        }
    }

    /**
     * Handle failed delivery with retry logic
     */
    private void handleFailedDelivery(WebhookDelivery delivery, String errorMessage) {
        delivery.setErrorMessage(errorMessage);

        if (delivery.getAttempts() >= delivery.getMaxAttempts()) {
            delivery.setStatus(DeliveryStatus.EXHAUSTED);
            log.warn("Webhook delivery exhausted after {} attempts: {}",
                    delivery.getAttempts(), delivery.getId());
        } else {
            delivery.setStatus(DeliveryStatus.RETRYING);

            // Calculate exponential backoff: 1min, 5min, 15min
            int delayMinutes = (int) Math.pow(5, delivery.getAttempts());
            delivery.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));

            log.info("Webhook will retry in {} minutes", delayMinutes);
        }
    }

    /**
     * Scheduled job to retry failed webhooks
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    @Transactional
    public void retryFailedWebhooks() {
        List<WebhookDelivery> retryQueue = deliveryRepository
                .findByStatusAndNextRetryAtBefore(DeliveryStatus.RETRYING, LocalDateTime.now());

        if (!retryQueue.isEmpty()) {
            log.info("Retrying {} failed webhooks", retryQueue.size());

            for (WebhookDelivery delivery : retryQueue) {
                // Get subscription to get secret
                WebhookSubscription subscription = subscriptionRepository
                        .findByWebhookUrl(delivery.getWebhookUrl())
                        .orElse(null);

                if (subscription != null && subscription.getActive()) {
                    attemptDelivery(delivery, subscription.getSecret());
                } else {
                    delivery.setStatus(DeliveryStatus.FAILED);
                    delivery.setErrorMessage("Subscription not found or inactive");
                    deliveryRepository.save(delivery);
                }
            }
        }
    }

    /**
     * Generate HMAC signature for webhook payload
     */
    private String generateSignature(String payload, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            hmac.init(secretKey);

            byte[] hash = hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Get delivery statistics
     */
    public java.util.Map<String, Object> getDeliveryStats() {
        long totalDeliveries = deliveryRepository.count();
        long successfulDeliveries = deliveryRepository.countByStatus(DeliveryStatus.DELIVERED);
        long failedDeliveries = deliveryRepository.countByStatus(DeliveryStatus.FAILED);
        long retrying = deliveryRepository.countByStatus(DeliveryStatus.RETRYING);

        return java.util.Map.of(
                "totalDeliveries", totalDeliveries,
                "successfulDeliveries", successfulDeliveries,
                "failedDeliveries", failedDeliveries,
                "retrying", retrying,
                "successRate", totalDeliveries > 0
                        ? (double) successfulDeliveries / totalDeliveries * 100
                        : 0.0
        );
    }
}