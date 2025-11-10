package com.example.ecommerce.webhook.controller;

import com.example.ecommerce.webhook.model.ChangeDetails;
import com.example.ecommerce.webhook.model.Product;
import com.example.ecommerce.webhook.model.ProductStatus;
import com.example.ecommerce.webhook.service.ProductService;
import com.example.ecommerce.webhook.service.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final WebhookDispatcher webhookDispatcher;

    /**
     * CREATE - Triggers 'product.created' webhook
     * POST /api/products
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        log.info("Creating new product: {}", product.getName());

        Product created = productService.createProduct(product);

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.created", created, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * READ - Get all products
     * GET /api/products
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status) {

        List<Product> products;

        if (category != null) {
            products = productService.getProductsByCategory(category);
        } else if (status != null) {
            products = productService.getProductsByStatus(status);
        } else {
            products = productService.getAllProducts();
        }

        return ResponseEntity.ok(products);
    }

    /**
     * READ - Get product by ID
     * GET /api/products/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * READ - Get product by SKU
     * GET /api/products/sku/{sku}
     */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        return productService.getProductBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * UPDATE - Triggers 'product.updated' webhook
     * PUT /api/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @RequestBody Product productDetails) {

        log.info("Updating product: {}", id);

        Product updated = productService.updateProduct(id, productDetails);

        // Trigger webhook with change details
        webhookDispatcher.dispatchProductEvent("product.updated", updated,
                ChangeDetails.builder()
                        .changeType("full_update")
                        .build()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * PARTIAL UPDATE - Update specific fields
     * PATCH /api/products/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<Product> patchProduct(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {

        log.info("Patching product: {} with fields: {}", id, updates.keySet());

        Product patched = productService.patchProduct(id, updates);

        webhookDispatcher.dispatchProductEvent("product.updated", patched,
                ChangeDetails.builder()
                        .changeType("partial_update")
                        .build()
        );

        return ResponseEntity.ok(patched);
    }

    /**
     * DELETE - Triggers 'product.deleted' webhook
     * DELETE /api/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product: {}", id);

        Product product = productService.getProductById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        productService.deleteProduct(id);

        // Trigger webhook
        webhookDispatcher.dispatchProductEvent("product.deleted", product, null);

        return ResponseEntity.noContent().build();
    }

    /**
     * UPDATE STOCK - Triggers 'product.stock.updated' webhook
     * POST /api/products/{id}/stock
     */
    @PostMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable Long id,
            @RequestBody Map<String, Object> stockUpdate) {

        Integer quantity = (Integer) stockUpdate.get("quantity");
        String operation = stockUpdate.getOrDefault("operation", "set").toString();

        log.info("Updating stock for product {}: {} {}", id, operation, quantity);

        Product updated = productService.updateStock(id, quantity, operation);

        // Trigger specific stock webhook
        webhookDispatcher.dispatchProductEvent("product.stock.updated", updated,
                ChangeDetails.builder()
                        .changeType("stock_change")
                        .oldStock(updated.getOldStock())
                        .newStock(updated.getStockQuantity())
                        .reason(operation)
                        .build()
        );

        // Check for low stock alert
        if (updated.getStockQuantity() <= updated.getLowStockThreshold()) {
            webhookDispatcher.dispatchProductEvent("product.stock.low", updated,
                    ChangeDetails.builder()
                            .changeType("low_stock_alert")
                            .newStock(updated.getStockQuantity())
                            .reason("Stock below threshold")
                            .build()
            );
        }

        return ResponseEntity.ok(updated);
    }

    /**
     * UPDATE PRICE - Triggers 'product.price.changed' webhook
     * POST /api/products/{id}/price
     */
    @PostMapping("/{id}/price")
    public ResponseEntity<Product> updatePrice(
            @PathVariable Long id,
            @RequestBody Map<String, Object> priceUpdate) {

        Double newPrice = ((Number) priceUpdate.get("price")).doubleValue();
        String reason = (String) priceUpdate.getOrDefault("reason", "price_update");

        log.info("Updating price for product {}: ${}", id, newPrice);

        Product updated = productService.updatePrice(id, newPrice);

        // Trigger price change webhook
        webhookDispatcher.dispatchProductEvent("product.price.changed", updated,
                ChangeDetails.builder()
                        .changeType("price_change")
                        .oldPrice(updated.getOldPrice())
                        .newPrice(updated.getPrice())
                        .reason(reason)
                        .build()
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * BULK OPERATIONS - Update multiple products
     * POST /api/products/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkUpdate(
            @RequestBody Map<String, Object> bulkOperation) {

        String operation = (String) bulkOperation.get("operation");
        List<Long> productIds = (List<Long>) bulkOperation.get("productIds");

        log.info("Bulk operation '{}' on {} products", operation, productIds.size());

        int successCount = 0;
        int failureCount = 0;

        for (Long id : productIds) {
            try {
                Product product = switch (operation) {
                    case "activate" -> productService.updateStatus(id, ProductStatus.ACTIVE);
                    case "deactivate" -> productService.updateStatus(id, ProductStatus.INACTIVE);
                    case "discount" -> {
                        Double discountPercent = ((Number) bulkOperation.get("discountPercent")).doubleValue();
                        yield productService.applyDiscount(id, discountPercent);
                    }
                    default -> null;
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

        return ResponseEntity.ok(Map.of(
                "operation", operation,
                "totalProducts", productIds.size(),
                "successCount", successCount,
                "failureCount", failureCount
        ));
    }

    /**
     * Search products
     * GET /api/products/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(
            @RequestParam String query) {

        List<Product> results = productService.searchProducts(query);
        return ResponseEntity.ok(results);
    }
}