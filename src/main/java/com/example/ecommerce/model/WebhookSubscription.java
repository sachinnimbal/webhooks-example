package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
