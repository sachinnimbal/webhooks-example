package com.example.ecommerce.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestWebhookResult {
    private String message;
    private String webhookUrl;
    private String note;
}
