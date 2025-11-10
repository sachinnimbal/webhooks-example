package com.example.ecommerce.graphql.input;

import com.example.ecommerce.webhook.model.ProductStatus;
import lombok.Data;

@Data
public class UpdateProductInput {
    private String sku;
    private String name;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private Integer lowStockThreshold;
    private ProductStatus status;
    private String category;
    private String brand;
    private String imageUrl;
}
