package com.example.ecommerce.webhook.respository;

import com.example.ecommerce.webhook.model.DeliveryStatus;
import com.example.ecommerce.webhook.model.WebhookDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WebhookDeliveryRepository extends JpaRepository<WebhookDelivery, Long> {

    List<WebhookDelivery> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<WebhookDelivery> findByWebhookUrlOrderByCreatedAtDesc(
            String webhookUrl, Pageable pageable);

    List<WebhookDelivery> findByStatus(DeliveryStatus status, Pageable pageable);

    List<WebhookDelivery> findByStatusAndNextRetryAtBefore(
            DeliveryStatus status, LocalDateTime nextRetryAt);

    long countByStatus(DeliveryStatus status);

    @Query("SELECT d FROM WebhookDelivery d WHERE d.eventType = :eventType " +
            "ORDER BY d.createdAt DESC")
    List<WebhookDelivery> findByEventType(
            @Param("eventType") String eventType, Pageable pageable);
}
