package com.example.ecommerce.webhook.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    private Integer lowStockThreshold;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private String category;
    private String brand;
    private String imageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Track what changed for webhook payload
    @Transient
    private String changeType;

    @Transient
    private BigDecimal oldPrice;

    @Transient
    private Integer oldStock;

    public OffsetDateTime getCreatedAt() {
        return createdAt != null ? createdAt.atOffset(ZoneOffset.UTC) : null;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt != null ? updatedAt.atOffset(ZoneOffset.UTC) : null;
    }
}

