package com.example.ecommerce.respository;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByCategory(String category);

    List<Product> findByStatus(ProductStatus status);

    @Query("SELECT p FROM Product p WHERE p.stockQuantity <= p.lowStockThreshold")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE " +
            "LOWER(p.name) LIKE %:query% OR " +
            "LOWER(p.description) LIKE %:query% OR " +
            "LOWER(p.sku) LIKE %:query% OR " +
            "LOWER(p.category) LIKE %:query%")
    List<Product> searchProducts(@Param("query") String query);
}