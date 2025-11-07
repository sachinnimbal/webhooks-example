# Product Webhook System 🚀

A **demonstration project** built to explain webhook concepts and implementation to the team. This is a complete, working example of a webhook system for e-commerce product events, showcasing real-time notifications, retry mechanisms, and secure payload delivery.

> **Note**: This is an educational/reference project created for internal team learning. It demonstrates production-ready webhook patterns but is not intended for direct production use.

---

## 📋 Table of Contents
- [What are Webhooks?](#-what-are-webhooks)
- [Why This Project?](#-why-this-project)
- [What's Covered](#-whats-covered)
- [Getting Started](#-getting-started)
- [Project Structure](#-project-structure)
- [API Documentation](#-api-documentation)
- [Live Demo](#-live-demo)
- [Key Concepts Explained](#-key-concepts-explained)
- [Learning Resources](#-learning-resources)

---

## 🔔 What are Webhooks?

**Webhooks** are HTTP callbacks that enable real-time, event-driven communication between systems. Think of them as "reverse APIs" - instead of you constantly asking for updates, the server notifies you when something happens.

### Real-World Example:
```
❌ WITHOUT WEBHOOKS (Polling)
You: "Is my Swiggy order ready?"
App: *Checks every 5 seconds*
Server: "No... No... No... Yes!"
Result: 100 unnecessary API calls!

✅ WITH WEBHOOKS
You: "Call me when my order is ready"
App: *Relaxes*
Server: *Calls you instantly when ready*
Result: 1 API call! Real-time notification!
```

### Where You See Webhooks Every Day:
- 💰 **Payment Apps** - "₹500 credited to your account" (Paytm, PhonePe, Razorpay)
- 🍕 **Food Delivery** - "Your order is out for delivery" (Swiggy, Zomato)
- 📦 **E-commerce** - "Your package is delivered" (Amazon, Flipkart)
- 💬 **Messaging** - "You have a new message" (WhatsApp, Slack)
- 🎵 **Subscriptions** - "Payment successful" (Spotify, Netflix)

---

## 🎯 Why This Project?

This project was created to:

1. **Explain webhooks** to the team with a working example
2. **Demonstrate best practices** for webhook implementation
3. **Show production-ready patterns** (security, retry logic, monitoring)
4. **Provide reference code** for future implementations
5. **Enable hands-on learning** with a complete system

---

## ✨ What's Covered

This project demonstrates **all essential webhook concepts**:

### Core Concepts
✅ **Event-Driven Architecture** - How to trigger webhooks on system events  
✅ **Subscription Management** - Registering webhook endpoints  
✅ **Payload Design** - Structuring webhook data  
✅ **Delivery System** - Sending HTTP POST requests  

### Security
✅ **HMAC-SHA256 Signatures** - Payload verification  
✅ **Secret Management** - Secure subscription secrets  
✅ **Signature Verification** - Preventing fake webhooks  

### Reliability
✅ **Automatic Retry Logic** - Exponential backoff (1min, 5min, 15min)  
✅ **Delivery Tracking** - Complete audit trail  
✅ **Status Management** - PENDING, DELIVERED, FAILED, RETRYING, EXHAUSTED  
✅ **Scheduled Retry Jobs** - Background processing  

### Monitoring & Testing
✅ **Delivery Statistics** - Success rates, counts  
✅ **Test Endpoints** - Built-in webhook receiver  
✅ **Comprehensive Logging** - Debug-friendly output  
✅ **Real-time Dashboard** - React frontend for monitoring  

### 7 Product Lifecycle Events
```
📦 product.created        → New product added
📝 product.updated        → Product modified
🗑️ product.deleted        → Product removed
📊 product.stock.updated  → Stock quantity changed
⚠️ product.stock.low      → Stock below threshold
💰 product.price.changed  → Price updated
📋 product.bulk.updated   → Bulk operations completed
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 17** or higher
- **Maven 3.6+**
- **Git**
- **Node.js 16+** (for frontend)

### Clone the Repository

```bash
# Clone from GitHub
git clone https://github.com/sachinnimbal/webhooks-example.git
cd webhooks-example
```

### Run the Backend (Spring Boot)

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Alternative: Run the JAR
java -jar target/ecommerce-0.0.1-SNAPSHOT.jar
```

**Backend will start at:** `http://localhost:8080`

### Run the Frontend (React + Vite)

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

**Frontend will start at:** `http://localhost:5173`

### Verify Installation

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Access H2 Database Console
# Open browser: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:productdb
# Username: sa
# Password: (leave empty)

# Access Frontend Dashboard
# Open browser: http://localhost:5173
```

---

## 📁 Project Structure

```
webhooks-example/
├── src/main/java/com/example/ecommerce/
│   ├── controller/
│   │   ├── ProductController.java              # Product CRUD + Webhook triggers
│   │   ├── WebhookSubscriptionController.java  # Manage subscriptions
│   │   └── WebhookReceiverController.java      # Test receiver endpoint
│   ├── service/
│   │   ├── ProductService.java                 # Business logic
│   │   └── WebhookDispatcher.java              # ⭐ Core webhook delivery logic
│   ├── model/
│   │   ├── Product.java                        # Product entity
│   │   ├── WebhookSubscription.java            # Subscription entity
│   │   ├── WebhookDelivery.java                # Delivery tracking entity
│   │   └── ProductWebhookPayload.java          # Webhook payload structure
│   └── repository/                             # JPA repositories
├── frontend/
│   └── src/
│       └── App.tsx                             # React dashboard
├── README.md                                   # This file
└── build.gradle                                # Dependencies
```

---

## 📡 API Documentation

### Quick Start: Register a Webhook

```bash
# 1. Create a webhook subscription
curl -X POST http://localhost:8080/api/webhooks/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Inventory System",
    "webhookUrl": "http://localhost:8080/test/webhook-receiver",
    "subscribedEvents": [
      "product.created",
      "product.stock.updated",
      "product.stock.low"
    ]
  }'

# Response includes a SECRET for signature verification
{
  "id": 1,
  "secret": "a1b2c3d4e5f6789...",
  "active": true,
  "webhookUrl": "http://localhost:8080/test/webhook-receiver",
  "subscribedEvents": ["product.created", "product.stock.updated"]
}
```

### Create a Product (Triggers Webhook!)

```bash
# 2. Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "LAPTOP-001",
    "name": "MacBook Pro 14",
    "description": "M3 Pro chip",
    "price": 199999,
    "stockQuantity": 50,
    "lowStockThreshold": 10,
    "category": "Electronics",
    "status": "ACTIVE"
  }'

# ✨ Webhook fires automatically!
# Check logs: You'll see "📨 WEBHOOK RECEIVED"
```

### What the Webhook Looks Like

```json
POST http://localhost:8080/test/webhook-receiver

Headers:
X-Webhook-Signature: wXy1z2A3B4C5D6E7F8G9H0...
X-Event-Type: product.created
X-Delivery-ID: 123
X-Attempt: 1

Body:
{
  "eventType": "product.created",
  "eventId": 1699999999000,
  "timestamp": 1699999999000,
  "data": {
    "id": 1,
    "sku": "LAPTOP-001",
    "name": "MacBook Pro 14",
    "price": 199999,
    "stockQuantity": 50,
    "status": "ACTIVE",
    "category": "Electronics"
  },
  "changes": null
}
```

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/webhooks/subscriptions` | Register webhook |
| `GET` | `/api/webhooks/subscriptions` | List all subscriptions |
| `PATCH` | `/api/webhooks/subscriptions/{id}/toggle` | Activate/deactivate |
| `POST` | `/api/webhooks/subscriptions/{id}/test` | Send test webhook |
| `GET` | `/api/webhooks/deliveries` | View delivery history |
| `GET` | `/api/webhooks/stats` | Delivery statistics |
| `POST` | `/api/products` | Create product (triggers webhook) |
| `POST` | `/api/products/{id}/stock` | Update stock (triggers webhook) |
| `POST` | `/api/products/{id}/price` | Update price (triggers webhook) |

**See full API docs in the project's `README.md` (original) for all endpoints.**

---

## 🎬 Live Demo

### Option 1: Use Built-in Test Endpoint

```bash
# Register subscription (already done in setup)
# Create product and watch console logs

# You'll see:
========================================
📨 WEBHOOK RECEIVED
========================================
Event Type: product.created
Delivery ID: 1
Payload: {...}
========================================
```

### Option 2: Use webhook.site

```bash
# 1. Go to https://webhook.site
# 2. Copy your unique URL (e.g., https://webhook.site/abc123)

# 3. Create subscription
curl -X POST http://localhost:8080/api/webhooks/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Webhook.site Test",
    "webhookUrl": "https://webhook.site/abc123",
    "subscribedEvents": ["product.created"]
  }'

# 4. Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-001",
    "name": "Test Product",
    "price": 99.99,
    "stockQuantity": 100
  }'

# 5. Go to webhook.site - you'll see the webhook instantly! ⚡
```

### Option 3: Use the Dashboard

1. Open `http://localhost:5173`
2. Click "Add Subscription"
3. Enter webhook URL
4. Click "Add Product"
5. Watch real-time webhook deliveries in the "Deliveries" tab! 🎉

---

## 🔑 Key Concepts Explained

### 1. Signature Verification (Security)

**Why?** Prevent fake webhooks from malicious actors.

**How it works:**
```java
// Server creates signature
HMAC-SHA256(payload + secret) → signature

// You verify it
Your HMAC-SHA256(payload + secret) → your_signature
if (signature == your_signature) ✅ TRUST IT
else ❌ REJECT IT
```

**Implementation:**
```javascript
// Node.js example
const crypto = require('crypto');

function verifyWebhook(payload, signature, secret) {
  const hmac = crypto.createHmac('sha256', secret);
  const computedSignature = hmac.update(payload).digest('base64');
  return computedSignature === signature;
}

// In your webhook endpoint
if (verifyWebhook(req.body, req.headers['x-webhook-signature'], secret)) {
  // ✅ Process webhook
} else {
  // ❌ Reject - it's fake!
}
```

### 2. Retry Logic (Reliability)

**What happens if your server is down?**

```
Attempt 1: ❌ FAILED → Retry in 1 minute
Attempt 2: ❌ FAILED → Retry in 5 minutes
Attempt 3: ❌ FAILED → Retry in 15 minutes
Attempt 4: 💀 EXHAUSTED → Manual intervention needed
```

**Why exponential backoff?**
- Give your server time to recover
- Avoid hammering a down system
- Standard industry practice

### 3. Event Types (Flexibility)

Subscribers choose **which events** they want:

```json
{
  "subscribedEvents": [
    "product.created",      // Only new products
    "product.stock.low"     // Only low stock alerts
  ]
}
```

**Result:** Inventory system gets stock alerts, Payment system gets price changes, Marketing gets new products - everyone gets only what they need!

### 4. Idempotency (Safety)

**Problem:** What if webhook is delivered twice?

**Solution:** Include `eventId` in payload:

```json
{
  "eventId": 1699999999000,  // Unique ID
  "eventType": "product.created",
  "data": {...}
}
```

**Your code:**
```java
if (alreadyProcessed(eventId)) {
    return; // Skip - already handled!
}
processEvent(event);
markAsProcessed(eventId);
```

---

## 📚 Learning Resources

### Included in This Project

1. **Working Code** - Fully commented, production-ready implementation
2. **Frontend Dashboard** - Visual tool to understand webhook flow

### External Resources

- **Testing Tools**
  - [webhook.site](https://webhook.site) - Free webhook testing
  - [ngrok](https://ngrok.com) - Expose localhost to internet
  - [Postman](https://postman.com) - API testing

- **Documentation**
  - [Stripe Webhooks](https://stripe.com/docs/webhooks) - Industry best practices
  - [GitHub Webhooks](https://docs.github.com/en/webhooks) - Real-world examples
  - [Webhook.fyi](https://webhook.fyi) - Webhook standards

### Advantages of Webhooks

✅ **Real-time updates** - Instant notifications  
✅ **No repeated API calls** - Efficient use of resources  
✅ **Saves bandwidth & server load** - Reduced traffic  
✅ **Easy automation** - Trigger workflows automatically  
✅ **Lightweight & scalable** - Simple HTTP requests  

### Disadvantages of Webhooks

❌ **No guaranteed delivery** - If your server is offline, you might miss it  
❌ **Needs secure setup** - HTTPS + signature verification required  
❌ **Harder to debug** - External trigger, not in your control  
❌ **Requires public URL** - Must be accessible from internet  
❌ **Events may arrive out of order** - Use timestamps/IDs  

---

## 🎓 Next Steps for Learning

### For Beginners:
1. ✅ Clone and run this project
2. ✅ Create a product and watch webhooks fire
3. ✅ Use webhook.site to see payloads
4. ✅ Read the presentation slides (`gradle/ppt-slides.md`)
5. ✅ Experiment with different events

### For Intermediate:
1. ✅ Understand signature verification code
2. ✅ Test retry logic (use `/test/webhook-receiver/fail`)
3. ✅ Add a new event type (e.g., `product.reviewed`)
4. ✅ Build a simple webhook consumer in Node.js/Python
5. ✅ Implement idempotency in your consumer

### For Advanced:
1. ✅ Add rate limiting to prevent abuse
2. ✅ Implement webhook replay functionality
3. ✅ Add filtering (e.g., only send if price > 1000)
4. ✅ Switch to persistent database (MySQL/PostgreSQL)
5. ✅ Add authentication (API keys, JWT)
6. ✅ Integrate with message queues (RabbitMQ, Kafka)

---

## 🤝 For the Team

### Using This Project

- **Reference Implementation** - Use this as a template for adding webhooks to our projects
- **Code Reviews** - Point to this repo for webhook best practices
- **Onboarding** - Share with new team members to learn webhooks
- **Presentations** - Use the slides for knowledge sharing sessions

### Questions?

This is a learning project. Feel free to:
- Experiment with the code
- Break things and see what happens
- Modify and extend functionality
- Ask questions in team channels

### Contributing to Learning

If you add new concepts or improvements, consider updating:
- This README with new insights
- The presentation slides with examples
- Code comments with explanations

---

## 🔧 Troubleshooting

**Webhooks not firing?**
```bash
# Check if subscription is active
curl http://localhost:8080/api/webhooks/subscriptions

# Check delivery logs
curl http://localhost:8080/api/webhooks/deliveries
```

**Frontend not connecting to backend?**
```bash
# Check CORS configuration in WebConfig.java
# Ensure frontend URL is: http://localhost:5173
```

**Signature verification failing?**
```bash
# Make sure you're using the correct secret from subscription response
# Secret changes if you regenerate it
```

**Retries not working?**
```bash
# Check logs for: "Retrying X failed webhooks"
# Scheduled job runs every 60 seconds
```

---

## 📝 Technical Stack

**Backend:**
- Spring Boot 3.5.7
- Java 17
- H2 Database (in-memory)
- Lombok
- Jackson
- RestTemplate

**Frontend:**
- React 19
- Vite
- Tailwind CSS 4
- Lucide Icons
- React Hot Toast

**Tools:**
- Maven
- Git
- webhook.site (for testing)

---

## 🎯 Summary

This project demonstrates:
- ✅ Complete webhook implementation from scratch
- ✅ Production-ready patterns (security, retry, monitoring)
- ✅ Real-time event-driven architecture
- ✅ Full-stack example with dashboard
- ✅ Comprehensive documentation and learning resources

**Perfect for:** Learning webhooks, reference implementation, team knowledge sharing, and understanding production webhook systems.

---

## 📧 Repository

**GitHub:** [https://github.com/sachinnimbal/webhooks-example](https://github.com/sachinnimbal/webhooks-example)

---

**Happy Learning! 🚀**

*"Don't call us, we'll call you!" - The Webhook Motto*
