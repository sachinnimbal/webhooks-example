package com.example.ecommerce.controller;

import com.example.ecommerce.model.*;
import com.example.ecommerce.respository.WebhookDeliveryRepository;
import com.example.ecommerce.respository.WebhookSubscriptionRepository;
import com.example.ecommerce.service.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller to manage webhook subscriptions
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookSubscriptionController {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;

    /**
     * CREATE - Register a new webhook subscription
     * POST /api/webhooks/subscriptions
     */
    @PostMapping("/subscriptions")
    public ResponseEntity<WebhookSubscription> createSubscription(
            @RequestBody WebhookSubscription subscription) {

        log.info("Creating webhook subscription: {}", subscription.getName());

        // Generate secret for signature verification
        subscription.setSecret(UUID.randomUUID().toString().replace("-", ""));
        subscription.setActive(true);
        subscription.setCreatedAt(LocalDateTime.now());

        WebhookSubscription created = subscriptionRepository.save(subscription);

        log.info("Subscription created with secret: {}", created.getSecret());

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * READ - Get all subscriptions
     * GET /api/webhooks/subscriptions
     */
    @GetMapping("/subscriptions")
    public ResponseEntity<List<WebhookSubscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionRepository.findAll());
    }

    /**
     * READ - Get subscription by ID
     * GET /api/webhooks/subscriptions/{id}
     */
    @GetMapping("/subscriptions/{id}")
    public ResponseEntity<WebhookSubscription> getSubscription(@PathVariable Long id) {
        return subscriptionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * UPDATE - Update subscription
     * PUT /api/webhooks/subscriptions/{id}
     */
    @PutMapping("/subscriptions/{id}")
    public ResponseEntity<WebhookSubscription> updateSubscription(
            @PathVariable Long id,
            @RequestBody WebhookSubscription subscriptionDetails) {

        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setName(subscriptionDetails.getName());
        subscription.setWebhookUrl(subscriptionDetails.getWebhookUrl());
        subscription.setSubscribedEvents(subscriptionDetails.getSubscribedEvents());
        subscription.setActive(subscriptionDetails.getActive());

        return ResponseEntity.ok(subscriptionRepository.save(subscription));
    }

    /**
     * DELETE - Remove subscription
     * DELETE /api/webhooks/subscriptions/{id}
     */
    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long id) {
        if (!subscriptionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        subscriptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ACTIVATE/DEACTIVATE subscription
     * PATCH /api/webhooks/subscriptions/{id}/toggle
     */
    @PatchMapping("/subscriptions/{id}/toggle")
    public ResponseEntity<WebhookSubscription> toggleSubscription(@PathVariable Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setActive(!subscription.getActive());

        log.info("Subscription {} is now {}", id,
                subscription.getActive() ? "ACTIVE" : "INACTIVE");

        return ResponseEntity.ok(subscriptionRepository.save(subscription));
    }

    /**
     * REGENERATE secret
     * POST /api/webhooks/subscriptions/{id}/regenerate-secret
     */
    @PostMapping("/subscriptions/{id}/regenerate-secret")
    public ResponseEntity<Map<String, String>> regenerateSecret(@PathVariable Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        String oldSecret = subscription.getSecret();
        String newSecret = UUID.randomUUID().toString().replace("-", "");

        subscription.setSecret(newSecret);
        subscriptionRepository.save(subscription);

        log.warn("Secret regenerated for subscription: {}", id);

        return ResponseEntity.ok(Map.of(
                "message", "Secret regenerated successfully",
                "newSecret", newSecret,
                "warning", "Update your webhook endpoint with the new secret"
        ));
    }

    /**
     * TEST webhook endpoint
     * POST /api/webhooks/subscriptions/{id}/test
     */
    @PostMapping("/subscriptions/{id}/test")
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        // Create a test payload
        ProductWebhookPayload testPayload = ProductWebhookPayload.builder()
                .eventType("webhook.test")
                .eventId(System.currentTimeMillis())
                .timestamp(System.currentTimeMillis())
                .data(ProductData.builder()
                        .id(0L)
                        .sku("TEST-SKU")
                        .name("Test Product")
                        .build())
                .build();

        // Send test webhook
        webhookDispatcher.deliverWebhook(subscription, testPayload);

        return ResponseEntity.ok(Map.of(
                "message", "Test webhook sent",
                "webhookUrl", subscription.getWebhookUrl(),
                "note", "Check your endpoint logs for the delivery"
        ));
    }

    /**
     * GET webhook deliveries for a subscription
     * GET /api/webhooks/subscriptions/{id}/deliveries
     */
    @GetMapping("/subscriptions/{id}/deliveries")
    public ResponseEntity<List<WebhookDelivery>> getSubscriptionDeliveries(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        List<WebhookDelivery> deliveries = deliveryRepository
                .findByWebhookUrlOrderByCreatedAtDesc(
                        subscription.getWebhookUrl(),
                        PageRequest.of(page, size)
                );

        return ResponseEntity.ok(deliveries);
    }

    /**
     * GET all webhook deliveries (monitoring)
     * GET /api/webhooks/deliveries
     */
    @GetMapping("/deliveries")
    public ResponseEntity<List<WebhookDelivery>> getAllDeliveries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) DeliveryStatus status) {

        List<WebhookDelivery> deliveries;

        if (status != null) {
            deliveries = deliveryRepository.findByStatus(status, PageRequest.of(page, size));
        } else {
            deliveries = deliveryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        }

        return ResponseEntity.ok(deliveries);
    }

    /**
     * GET webhook delivery statistics
     * GET /api/webhooks/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWebhookStats() {
        Map<String, Object> stats = webhookDispatcher.getDeliveryStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * RETRY failed webhook delivery
     * POST /api/webhooks/deliveries/{deliveryId}/retry
     */
    @PostMapping("/deliveries/{deliveryId}/retry")
    public ResponseEntity<Map<String, String>> retryDelivery(@PathVariable Long deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot retry successful delivery"));
        }

        // Reset for retry
        delivery.setStatus(DeliveryStatus.RETRYING);
        delivery.setNextRetryAt(LocalDateTime.now());
        delivery.setErrorMessage(null);
        deliveryRepository.save(delivery);

        log.info("Manual retry scheduled for delivery: {}", deliveryId);

        return ResponseEntity.ok(Map.of(
                "message", "Delivery scheduled for retry",
                "deliveryId", deliveryId.toString()
        ));
    }

    /**
     * LIST available webhook events
     * GET /api/webhooks/events
     */
    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> getAvailableEvents() {
        List<String> events = List.of(
                "product.created",
                "product.updated",
                "product.deleted",
                "product.stock.updated",
                "product.stock.low",
                "product.price.changed",
                "product.bulk.updated"
        );

        Map<String, String> descriptions = Map.of(
                "product.created", "Triggered when a new product is created",
                "product.updated", "Triggered when product details are updated",
                "product.deleted", "Triggered when a product is deleted",
                "product.stock.updated", "Triggered when stock quantity changes",
                "product.stock.low", "Triggered when stock falls below threshold",
                "product.price.changed", "Triggered when product price changes",
                "product.bulk.updated", "Triggered during bulk operations"
        );

        return ResponseEntity.ok(Map.of(
                "events", events,
                "descriptions", descriptions
        ));
    }
}