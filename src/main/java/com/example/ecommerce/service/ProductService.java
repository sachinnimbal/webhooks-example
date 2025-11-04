package com.example.ecommerce.service;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.ProductStatus;
import com.example.ecommerce.respository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(Product product) {
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(10);
        }

        // Check if stock is low on creation
        if (product.getStockQuantity() <= product.getLowStockThreshold()) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    public List<Product> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status);
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Track changes for webhook
        product.setOldPrice(product.getPrice());
        product.setOldStock(product.getStockQuantity());

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setBrand(productDetails.getBrand());
        product.setImageUrl(productDetails.getImageUrl());
        product.setLowStockThreshold(productDetails.getLowStockThreshold());

        if (productDetails.getStatus() != null) {
            product.setStatus(productDetails.getStatus());
        }

        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    @Transactional
    public Product patchProduct(Long id, Map<String, Object> updates) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        updates.forEach((key, value) -> {
            switch (key) {
                case "name":
                    product.setName((String) value);
                    break;
                case "description":
                    product.setDescription((String) value);
                    break;
                case "price":
                    product.setOldPrice(product.getPrice());
                    product.setPrice(new BigDecimal(value.toString()));
                    break;
                case "stockQuantity":
                    product.setOldStock(product.getStockQuantity());
                    product.setStockQuantity((Integer) value);
                    break;
                case "status":
                    product.setStatus(ProductStatus.valueOf((String) value));
                    break;
                case "category":
                    product.setCategory((String) value);
                    break;
                case "brand":
                    product.setBrand((String) value);
                    break;
                case "imageUrl":
                    product.setImageUrl((String) value);
                    break;
                case "lowStockThreshold":
                    product.setLowStockThreshold((Integer) value);
                    break;
            }
        });

        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public Product updateStock(Long id, Integer quantity, String operation) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setOldStock(product.getStockQuantity());

        switch (operation.toLowerCase()) {
            case "set":
                product.setStockQuantity(quantity);
                break;
            case "add":
                product.setStockQuantity(product.getStockQuantity() + quantity);
                break;
            case "subtract":
                int newStock = product.getStockQuantity() - quantity;
                product.setStockQuantity(Math.max(0, newStock));
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + operation);
        }

        // Auto-update status based on stock
        if (product.getStockQuantity() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        product.setUpdatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product updatePrice(Long id, Double newPrice) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setOldPrice(product.getPrice());
        product.setPrice(BigDecimal.valueOf(newPrice));
        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    @Transactional
    public Product updateStatus(Long id, ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    @Transactional
    public Product applyDiscount(Long id, Double discountPercent) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setOldPrice(product.getPrice());

        BigDecimal discount = product.getPrice()
                .multiply(BigDecimal.valueOf(discountPercent / 100));
        BigDecimal newPrice = product.getPrice().subtract(discount);

        product.setPrice(newPrice);
        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    public List<Product> searchProducts(String query) {
        return productRepository.searchProducts(query.toLowerCase());
    }

    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }
}