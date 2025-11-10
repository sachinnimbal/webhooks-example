package com.example.ecommerce.graphql.resolver;

import com.example.ecommerce.graphql.input.CreateWebhookSubscriptionInput;
import com.example.ecommerce.graphql.input.UpdateWebhookSubscriptionInput;
import com.example.ecommerce.graphql.type.*;
import com.example.ecommerce.webhook.model.*;
import com.example.ecommerce.webhook.respository.WebhookDeliveryRepository;
import com.example.ecommerce.webhook.respository.WebhookSubscriptionRepository;
import com.example.ecommerce.webhook.service.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebhookResolver {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookDispatcher webhookDispatcher;

    // ========== QUERIES ==========

    @QueryMapping
    public List<WebhookSubscription> webhookSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @QueryMapping
    public WebhookSubscription webhookSubscription(@Argument Long id) {
        return subscriptionRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public List<WebhookDelivery> webhookDeliveries(
            @Argument Integer page,
            @Argument Integer size,
            @Argument DeliveryStatus status) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 50;

        if (status != null) {
            return deliveryRepository.findByStatus(status, PageRequest.of(pageNum, pageSize));
        } else {
            return deliveryRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(pageNum, pageSize));
        }
    }

    @QueryMapping
    public WebhookDelivery webhookDelivery(@Argument Long id) {
        return deliveryRepository.findById(id).orElse(null);
    }

    @QueryMapping
    public WebhookStats webhookStats() {
        Map<String, Object> stats = webhookDispatcher.getDeliveryStats();

        return WebhookStats.builder()
                .totalDeliveries(((Number) stats.get("totalDeliveries")).intValue())
                .successfulDeliveries(((Number) stats.get("successfulDeliveries")).intValue())
                .failedDeliveries(((Number) stats.get("failedDeliveries")).intValue())
                .retrying(((Number) stats.get("retrying")).intValue())
                .successRate(((Number) stats.get("successRate")).doubleValue())
                .build();
    }

    @QueryMapping
    public EventInfo availableEvents() {
        List<String> events = List.of(
                "product.created",
                "product.updated",
                "product.deleted",
                "product.stock.updated",
                "product.stock.low",
                "product.price.changed",
                "product.bulk.updated"
        );

        Map<String, String> descMap = Map.of(
                "product.created", "Triggered when a new product is created",
                "product.updated", "Triggered when product details are updated",
                "product.deleted", "Triggered when a product is deleted",
                "product.stock.updated", "Triggered when stock quantity changes",
                "product.stock.low", "Triggered when stock falls below threshold",
                "product.price.changed", "Triggered when product price changes",
                "product.bulk.updated", "Triggered during bulk operations"
        );

        List<EventDescription> descriptions = events.stream()
                .map(event -> EventDescription.builder()
                        .event(event)
                        .description(descMap.get(event))
                        .build())
                .toList();

        return EventInfo.builder()
                .events(events)
                .descriptions(descriptions)
                .build();
    }

    // ========== MUTATIONS ==========

    @MutationMapping
    public WebhookSubscription createWebhookSubscription(@Argument CreateWebhookSubscriptionInput input) {
        log.info("GraphQL: Creating webhook subscription: {}", input.getName());

        WebhookSubscription subscription = WebhookSubscription.builder()
                .name(input.getName())
                .webhookUrl(input.getWebhookUrl())
                .secret(UUID.randomUUID().toString().replace("-", ""))
                .subscribedEvents(new HashSet<>(input.getSubscribedEvents()))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        return subscriptionRepository.save(subscription);
    }

    @MutationMapping
    public WebhookSubscription updateWebhookSubscription(
            @Argument Long id,
            @Argument UpdateWebhookSubscriptionInput input) {

        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (input.getName() != null) subscription.setName(input.getName());
        if (input.getWebhookUrl() != null) subscription.setWebhookUrl(input.getWebhookUrl());
        if (input.getSubscribedEvents() != null) {
            subscription.setSubscribedEvents(new HashSet<>(input.getSubscribedEvents()));
        }
        if (input.getActive() != null) subscription.setActive(input.getActive());

        return subscriptionRepository.save(subscription);
    }

    @MutationMapping
    public Boolean deleteWebhookSubscription(@Argument Long id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new RuntimeException("Subscription not found");
        }
        subscriptionRepository.deleteById(id);
        return true;
    }

    @MutationMapping
    public WebhookSubscription toggleWebhookSubscription(@Argument Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        subscription.setActive(!subscription.getActive());

        log.info("Subscription {} is now {}", id,
                subscription.getActive() ? "ACTIVE" : "INACTIVE");

        return subscriptionRepository.save(subscription);
    }

    @MutationMapping
    public SecretRegenerationResult regenerateWebhookSecret(@Argument Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        String newSecret = UUID.randomUUID().toString().replace("-", "");
        subscription.setSecret(newSecret);
        subscriptionRepository.save(subscription);

        log.warn("Secret regenerated for subscription: {}", id);

        return SecretRegenerationResult.builder()
                .message("Secret regenerated successfully")
                .newSecret(newSecret)
                .warning("Update your webhook endpoint with the new secret")
                .build();
    }

    @MutationMapping
    public TestWebhookResult testWebhook(@Argument Long id) {
        WebhookSubscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        // Create test payload
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

        return TestWebhookResult.builder()
                .message("Test webhook sent")
                .webhookUrl(subscription.getWebhookUrl())
                .note("Check your endpoint logs for the delivery")
                .build();
    }

    @MutationMapping
    public RetryResult retryWebhookDelivery(@Argument Long deliveryId) {
        WebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Delivery not found"));

        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            throw new RuntimeException("Cannot retry successful delivery");
        }

        // Reset for retry
        delivery.setStatus(DeliveryStatus.RETRYING);
        delivery.setNextRetryAt(LocalDateTime.now());
        delivery.setErrorMessage(null);
        deliveryRepository.save(delivery);

        log.info("Manual retry scheduled for delivery: {}", deliveryId);

        return RetryResult.builder()
                .message("Delivery scheduled for retry")
                .deliveryId(deliveryId.toString())
                .build();
    }
}