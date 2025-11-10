package com.example.ecommerce.graphql.type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateResult {
    private String operation;
    private Integer totalProducts;
    private Integer successCount;
    private Integer failureCount;
}