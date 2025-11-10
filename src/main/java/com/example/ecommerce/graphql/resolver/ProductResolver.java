package com.example.ecommerce.graphql.resolver;

import com.example.ecommerce.graphql.input.CreateProductInput;
import com.example.ecommerce.graphql.input.UpdateProductInput;
import com.example.ecommerce.graphql.type.BulkUpdateResult;
import com.example.ecommerce.webhook.model.ChangeDetails;
import com.example.ecommerce.webhook.model.Product;
import com.example.ecommerce.webhook.model.ProductStatus;
import com.example.ecommerce.webhook.service.ProductService;
import com.example.ecommerce.webhook.service.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProductResolver {

    private final ProductService productService;
    private final WebhookDispatcher webhookDispatcher;

    // ========== QUERIES ==========

    @QueryMapping
    public List<Product> products(
            @Argument String category,
            @Argument ProductStatus status) {

        if (category != null) {
            return productService.getProductsByCategory(category);
        } else if (status != null) {
            return productService.getProductsByStatus(status);
        } else {
            return productService.getAllProducts();
        }
    }

    @QueryMapping
    public Product product(@Argument Long id) {
        return productService.getProductById(id).orElse(null);
    }

    @QueryMapping
    public Product productBySku(@Argument String sku) {
        return productService.getProductBySku(sku).orElse(null);
    }

    @QueryMapping
    public List<Product> searchProducts(@Argument String query) {
        return productService.searchProducts(query);
    }

    @QueryMapping
    public List<Product> lowStockProducts() {
        return productService.getLowStockProducts();
    }

    // ========== MUTATIONS ==========

    @MutationMapping
    public Product createProduct(@Argument CreateProductInput input) {
        log.info("GraphQL: Creating product: {}", input.getName());

        Product product = Product.builder()
                .sku(input.getSku())
                .name(input.getName())
                .description(input.getDescription())
                .price(BigDecimal.valueOf(input.getPrice()))
                .stockQuantity(input.getStockQuantity())
                .lowStockThreshold(input.getLowStockThreshold() != null ? input.getLowStockThreshold() : 10)
                .status(input.getStatus() != null ? input.getStatus() : ProductStatus.ACTIVE)
                .category(input.getCategory())
                .brand(input.getBrand())
                .imageUrl(input.getImageUrl())
                .build();

        Product created = productService.createProduct(product);

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.created", created, null);

        return created;
    }

    @MutationMapping
    public Product updateProduct(@Argument Long id, @Argument UpdateProductInput input) {
        log.info("GraphQL: Updating product: {}", id);

        Product existing = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (input.getSku() != null) existing.setSku(input.getSku());
        if (input.getName() != null) existing.setName(input.getName());
        if (input.getDescription() != null) existing.setDescription(input.getDescription());
        if (input.getPrice() != null) {
            existing.setOldPrice(existing.getPrice());
            existing.setPrice(BigDecimal.valueOf(input.getPrice()));
        }
        if (input.getStockQuantity() != null) {
            existing.setOldStock(existing.getStockQuantity());
            existing.setStockQuantity(input.getStockQuantity());
        }
        if (input.getLowStockThreshold() != null) existing.setLowStockThreshold(input.getLowStockThreshold());
        if (input.getStatus() != null) existing.setStatus(input.getStatus());
        if (input.getCategory() != null) existing.setCategory(input.getCategory());
        if (input.getBrand() != null) existing.setBrand(input.getBrand());
        if (input.getImageUrl() != null) existing.setImageUrl(input.getImageUrl());

        Product updated = productService.updateProduct(id, existing);

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.updated", updated,
                ChangeDetails.builder()
                        .changeType("full_update")
                        .build()
        );

        return updated;
    }

    @MutationMapping
    public Boolean deleteProduct(@Argument Long id) {
        log.info("GraphQL: Deleting product: {}", id);

        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        productService.deleteProduct(id);

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.deleted", product, null);

        return true;
    }

    @MutationMapping
    public Product updateStock(
            @Argument Long id,
            @Argument Integer quantity,
            @Argument String operation) {

        log.info("GraphQL: Updating stock for product {}: {} {}", id, operation, quantity);

        Product updated = productService.updateStock(id, quantity, operation.toLowerCase());

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.stock.updated", updated,
                ChangeDetails.builder()
                        .changeType("stock_change")
                        .oldStock(updated.getOldStock())
                        .newStock(updated.getStockQuantity())
                        .reason(operation)
                        .build()
        );

        // Check for low stock
        if (updated.getStockQuantity() <= updated.getLowStockThreshold()) {
            webhookDispatcher.dispatchProductEvent("product.stock.low", updated,
                    ChangeDetails.builder()
                            .changeType("low_stock_alert")
                            .newStock(updated.getStockQuantity())
                            .reason("Stock below threshold")
                            .build()
            );
        }

        return updated;
    }

    @MutationMapping
    public Product updatePrice(
            @Argument Long id,
            @Argument Double price,
            @Argument String reason) {

        log.info("GraphQL: Updating price for product {}: ${}", id, price);

        Product updated = productService.updatePrice(id, price);

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.price.changed", updated,
                ChangeDetails.builder()
                        .changeType("price_change")
                        .oldPrice(updated.getOldPrice())
                        .newPrice(updated.getPrice())
                        .reason(reason != null ? reason : "price_update")
                        .build()
        );

        return updated;
    }

    @MutationMapping
    public BulkUpdateResult bulkUpdate(
            @Argument String operation,
            @Argument List<Long> productIds,
            @Argument Double discountPercent) {

        log.info("GraphQL: Bulk operation '{}' on {} products", operation, productIds.size());

        int successCount = 0;
        int failureCount = 0;

        for (Long id : productIds) {
            try {
                Product product = switch (operation) {
                    case "activate" -> productService.updateStatus(id, ProductStatus.ACTIVE);
                    case "deactivate" -> productService.updateStatus(id, ProductStatus.INACTIVE);
                    case "discount" -> {
                        if (discountPercent == null) {
                            throw new IllegalArgumentException("discountPercent required for discount operation");
                        }
                        yield productService.applyDiscount(id, discountPercent);
                    }
                    default -> throw new IllegalArgumentException("Unknown operation: " + operation);
                };

                if (product != null) {
                    webhookDispatcher.dispatchProductEvent("product.bulk.updated", product,
                            ChangeDetails.builder()
                                    .changeType(operation)
                                    .reason("bulk_operation")
                                    .build()
                    );
                    successCount++;
                }

            } catch (Exception e) {
                log.error("Failed to update product {}: {}", id, e.getMessage());
                failureCount++;
            }
        }

        return BulkUpdateResult.builder()
                .operation(operation)
                .totalProducts(productIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .build();
    }
}