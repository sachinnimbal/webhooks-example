package com.example.ecommerce.graphql.input;

import lombok.Data;

import java.util.List;

@Data
public class UpdateWebhookSubscriptionInput {
    private String name;
    private String webhookUrl;
    private List<String> subscribedEvents;
    private Boolean active;
}