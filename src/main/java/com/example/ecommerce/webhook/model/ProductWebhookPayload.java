package com.example.ecommerce.webhook.model;

import lombok.Builder;
import lombok.Data;

// === DTOs ===
@Data
@Builder
public class ProductWebhookPayload {
    private String eventType;
    private Long eventId;
    private Long timestamp;
    private ProductData data;
    private ChangeDetails changes;
}
