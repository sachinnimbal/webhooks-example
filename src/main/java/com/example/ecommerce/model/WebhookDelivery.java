package com.example.ecommerce.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
