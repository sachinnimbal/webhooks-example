package com.example.ecommerce.respository;

import com.example.ecommerce.model.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    List<WebhookSubscription> findByActive(Boolean active);

    List<WebhookSubscription> findByActiveAndSubscribedEventsContaining(
            Boolean active, String eventType);

    Optional<WebhookSubscription> findByWebhookUrl(String webhookUrl);
}
