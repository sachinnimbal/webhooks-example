package com.example.ecommerce.webhook.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// === Webhook Event Entity ===
@Entity
@Table(name = "webhook_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String webhookUrl;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private Integer attempts;
    private Integer maxAttempts;

    private String responseCode;
    private String errorMessage;

    private LocalDateTime createdAt;
    private LocalDateTime nextRetryAt;
    private LocalDateTime deliveredAt;

    public OffsetDateTime getCreatedAt() {
        return createdAt != null ? createdAt.atOffset(ZoneOffset.UTC) : null;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt != null ? nextRetryAt.atOffset(ZoneOffset.UTC) : null;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt != null ? deliveredAt.atOffset(ZoneOffset.UTC) : null;
    }
}
