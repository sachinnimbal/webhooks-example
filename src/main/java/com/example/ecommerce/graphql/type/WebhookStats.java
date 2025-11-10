package com.example.ecommerce.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookStats {
    private Integer totalDeliveries;
    private Integer successfulDeliveries;
    private Integer failedDeliveries;
    private Integer retrying;
    private Double successRate;
}
