package com.example.ecommerce.webhook.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// === Webhook Subscription Entity ===
@Entity
@Table(name = "webhook_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String webhookUrl;

    @Column(nullable = false)
    private String secret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "subscription_events")
    private java.util.Set<String> subscribedEvents;

    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime lastDeliveryAt;

    public OffsetDateTime getCreatedAt() {
        return createdAt != null ? createdAt.atOffset(ZoneOffset.UTC) : null;
    }

    public OffsetDateTime getLastDeliveryAt() {
        return lastDeliveryAt != null ? lastDeliveryAt.atOffset(ZoneOffset.UTC) : null;
    }

}
