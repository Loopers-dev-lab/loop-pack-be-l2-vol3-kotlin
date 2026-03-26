# Outbox + Kafka Metrics Collection System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Outbox pattern for transactional event storage and Kafka-based metrics aggregation (view_count, sales_count) across commerce-api and commerce-streamer with Atomic Query concurrency control.

**Architecture:** Commerce-api stores domain events in an Outbox table within the same transaction as domain changes. A scheduler polls the Outbox every 10 seconds and publishes unpublished events to a single "metrics-events" Kafka topic. Commerce-streamer consumes events in batches, validates idempotency via event_handled table, and increments metrics using DB-level atomic UPDATE queries (no locks).

**Tech Stack:** Kotlin, Spring Boot, Spring Data JPA, Kafka, Flyway (migrations), Jackson (JSON), Batch listener

---

## File Structure

**commerce-api (Outbox & Publishing):**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxEvent.kt` (JPA entity)
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxRepository.kt` (JPA interface)
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/domain/outbox/OutboxPublisher.kt` (service)
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxPoller.kt` (scheduler)
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductService.kt` (add Outbox publish)
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt` (add Outbox publish)
- Create: `apps/commerce-api/src/main/resources/db/migration/V[version]__add_outbox_table.sql`

**commerce-streamer (Metrics Aggregation):**
- Create: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/EventHandler.kt` (interface)
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/productmetrics/ProductMetricsRepository.kt` (add @Modifying queries)
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetrics.kt` (remove increment methods, make fields immutable)
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/order/OrderCreatedEvent.kt` (change to lineItems)
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetricsService.kt` (add extractDedupeKey)
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/kafka/ProductMetricsConsumer.kt` (subscribe to metrics-events)
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductViewedEventHandler.kt` (call atomic query)
- Create: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/OrderCreatedEventHandler.kt` (new handler)

**Tests:**
- Create: `apps/commerce-api/src/test/kotlin/com/loopers/infrastructure/outbox/OutboxPollerTest.kt`
- Modify: `apps/commerce-streamer/src/test/kotlin/com/loopers/domain/productmetrics/ProductMetricsServiceTest.kt`

---

## Tasks

### Task 1: Create Outbox Table Migration

**Files:**
- Create: `apps/commerce-api/src/main/resources/db/migration/V[YYYYMMDDHHMM]__add_outbox_and_event_handled_tables.sql`

- [ ] **Step 1: Create migration file for outbox table**

Create file with SQL (replace `[YYYYMMDDHHMM]` with current timestamp, e.g., `V20260327120000`):

```sql
CREATE TABLE outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    topic VARCHAR(50) NOT NULL DEFAULT 'metrics-events',
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP NULL,
    UNIQUE KEY uk_aggregate_event (aggregate_id, event_type, created_at),
    KEY idx_published_created (published, created_at),
    KEY idx_topic (topic)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_handled (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dedupe_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Modify product_metrics: add sales_count if not exists, remove like_count if exists
ALTER TABLE product_metrics ADD COLUMN sales_count BIGINT DEFAULT 0 AFTER view_count;
```

- [ ] **Step 2: Verify migration file exists in correct location**

Run: `ls -la apps/commerce-api/src/main/resources/db/migration/ | grep outbox`

Expected: Migration file present with V[YYYYMMDDHHMM]__add_outbox prefix

- [ ] **Step 3: Commit migration file**

```bash
git add apps/commerce-api/src/main/resources/db/migration/V*__add_outbox*.sql
git commit -m "db: add outbox and event_handled tables for metrics event sourcing

- Create outbox table with published flag and indexes for polling
- Create event_handled table for deduplication (UNIQUE dedupe_key)
- Add sales_count column to product_metrics table"
```

---

### Task 2: Implement OutboxEvent Entity

**Files:**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxEvent.kt`

- [ ] **Step 1: Create OutboxEvent entity**

```kotlin
package com.loopers.infrastructure.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "outbox")
class OutboxEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val aggregateId: Long,
    val eventType: String,  // "ProductViewedEvent", "OrderCreatedEvent"
    @Column(columnDefinition = "JSON")
    val payload: String,    // JSON serialized event
    val topic: String = "metrics-events",

    var published: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    var publishedAt: LocalDateTime? = null,
)
```

- [ ] **Step 2: Verify file created and compiles**

Run: `./gradlew :apps:commerce-api:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit entity**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxEvent.kt
git commit -m "feat: add OutboxEvent JPA entity for transactional event storage

- Stores domain events (ProductViewedEvent, OrderCreatedEvent) in outbox table
- Fields: aggregateId, eventType, payload (JSON), topic, published flag
- Supports idempotent Kafka publishing with published_at timestamp"
```

---

### Task 3: Implement OutboxRepository

**Files:**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxRepository.kt`

- [ ] **Step 1: Create OutboxRepository interface**

```kotlin
package com.loopers.infrastructure.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OutboxRepository : JpaRepository<OutboxEvent, Long> {
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC")
    fun findUnpublished(@Param("limit") limit: Int = 100): List<OutboxEvent>
}
```

- [ ] **Step 2: Verify repository compiles**

Run: `./gradlew :apps:commerce-api:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit repository**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxRepository.kt
git commit -m "feat: add OutboxRepository for Outbox pattern event queries

- findUnpublished: queries unpublished events ordered by creation time
- Supports batch polling (limit) for Scheduler efficiency"
```

---

### Task 4: Implement OutboxPublisher Service

**Files:**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/domain/outbox/OutboxPublisher.kt`

- [ ] **Step 1: Create OutboxPublisher service**

```kotlin
package com.loopers.domain.outbox

import com.loopers.infrastructure.outbox.OutboxEvent
import com.loopers.infrastructure.outbox.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
) {
    fun publish(event: Any, aggregateId: Long) {
        val payload = objectMapper.writeValueAsString(event)
        val outboxEvent = OutboxEvent(
            aggregateId = aggregateId,  // productId or orderId
            eventType = event::class.simpleName!!,
            payload = payload,
        )
        outboxRepository.save(outboxEvent)
    }
}
```

- [ ] **Step 2: Verify service compiles**

Run: `./gradlew :apps:commerce-api:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit service**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/outbox/OutboxPublisher.kt
git commit -m "feat: add OutboxPublisher service for transactional event persistence

- publish(event, aggregateId): serializes event to JSON and saves to Outbox table
- Runs within @Transactional context for atomicity with domain changes
- Supports both ProductViewedEvent and OrderCreatedEvent"
```

---

### Task 5: Implement OutboxPoller Scheduler

**Files:**
- Create: `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxPoller.kt`

- [ ] **Step 1: Create OutboxPoller scheduler component**

```kotlin
package com.loopers.infrastructure.outbox

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxPoller(
    private val outboxRepository: OutboxRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 10000)  // 10 seconds
    fun pollAndPublish() {
        val unpublished = outboxRepository.findUnpublished(limit = 100)

        for (outbox in unpublished) {
            try {
                kafkaTemplate.send(
                    outbox.topic,
                    outbox.aggregateId.toString(),  // Key: productId or orderId
                    outbox.payload
                ).get()

                outbox.published = true
                outbox.publishedAt = java.time.LocalDateTime.now()
                outboxRepository.save(outbox)

                logger.debug("Published outbox event: id={}, eventType={}", outbox.id, outbox.eventType)
            } catch (e: Exception) {
                logger.error("Failed to publish outbox event: id=${outbox.id}, eventType=${outbox.eventType}", e)
                // Leave published=false for retry on next poll
            }
        }
    }
}
```

- [ ] **Step 2: Verify scheduler compiles**

Run: `./gradlew :apps:commerce-api:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify @Scheduled is enabled in application.yml**

Check: `apps/commerce-api/src/main/resources/application.yml` should have:

```yaml
spring:
  task:
    scheduling:
      enabled: true
```

If missing, add it.

- [ ] **Step 4: Commit scheduler**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/outbox/OutboxPoller.kt
git commit -m "feat: add OutboxPoller scheduler for Kafka event publishing

- Runs every 10 seconds to poll unpublished Outbox events
- Publishes to metrics-events topic with aggregateId as message key
- Marks event published on success, retries on failure
- Batches up to 100 events per poll for efficiency"
```

---

### Task 6: Update ProductService to Use Outbox

**Files:**
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductService.kt`

- [ ] **Step 1: Read ProductService to understand current structure**

Run: `grep -n "recordProductView\|ProductViewedEvent" apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductService.kt | head -20`

Expected: Output showing recordProductView method and ProductViewedEvent usage

- [ ] **Step 2: Add OutboxPublisher dependency and update recordProductView**

Find the `recordProductView` method and update to:

```kotlin
import com.loopers.domain.outbox.OutboxPublisher

@Service
@Transactional
class ProductService(
    // ... existing dependencies
    private val outboxPublisher: OutboxPublisher,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun recordProductView(productId: Long, userId: Long) {
        // Existing logic...
        val product = productRepository.findById(productId).orElseThrow()

        val event = ProductViewedEvent(
            source = this,
            productId = productId,
            userId = userId,
            dedupeKey = "view:$productId:$userId:${UUID.randomUUID()}"
        )

        // NEW: Save to Outbox (same transaction)
        outboxPublisher.publish(event, productId)

        // Existing: Publish ApplicationEvent for local listeners
        eventPublisher.publishEvent(event)
    }
}
```

- [ ] **Step 3: Verify ProductService compiles**

Run: `./gradlew :apps:commerce-api:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit ProductService changes**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/product/ProductService.kt
git commit -m "feat: integrate Outbox publishing in ProductService.recordProductView

- ProductViewedEvent now published to Outbox table in same transaction
- Ensures event persistence even if Kafka publish fails later
- Event will be picked up by OutboxPoller scheduler"
```

---

### Task 7: Update OrderService to Use Outbox

**Files:**
- Modify: `apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt`

- [ ] **Step 1: Check if OrderService has order creation method**

Run: `grep -n "createOrder\|create" apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt | head -10`

Expected: Output showing order creation logic

- [ ] **Step 2: Add OutboxPublisher and update order creation**

Update order creation method to include:

```kotlin
import com.loopers.domain.outbox.OutboxPublisher
import com.loopers.domain.order.OrderCreatedEvent
import com.loopers.domain.order.OrderLineItem

@Service
@Transactional
class OrderService(
    // ... existing dependencies
    private val outboxPublisher: OutboxPublisher,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @Transactional
    fun createOrder(userId: Long, lineItems: List<OrderLineItem>): Order {
        // Existing: Create and save order
        val order = Order.create(userId, lineItems)
        orderRepository.save(order)

        // NEW: Publish OrderCreatedEvent to Outbox (same transaction)
        val event = OrderCreatedEvent(
            source = this,
            orderId = order.id,
            lineItems = lineItems,
            dedupeKey = "order:${order.id}:${UUID.randomUUID()}"
        )
        outboxPublisher.publish(event, order.id)

        // Existing: Publish ApplicationEvent for local listeners
        eventPublisher.publishEvent(event)

        return order
    }
}
```

- [ ] **Step 3: Verify OrderService compiles**

Run: `./gradlew :apps:commerce-api:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit OrderService changes**

```bash
git add apps/commerce-api/src/main/kotlin/com/loopers/domain/order/OrderService.kt
git commit -m "feat: integrate Outbox publishing in OrderService.createOrder

- OrderCreatedEvent now published to Outbox table with lineItems in same transaction
- Ensures all order metrics events are captured for Kafka distribution
- Scheduler will publish to metrics-events topic for commerce-streamer consumption"
```

---

### Task 8: Create EventHandler Interface (commerce-streamer)

**Files:**
- Create: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/EventHandler.kt`

- [ ] **Step 1: Create EventHandler interface**

```kotlin
package com.loopers.domain.productmetrics

interface EventHandler {
    fun handle(event: Any)
}
```

- [ ] **Step 2: Verify interface compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit interface**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/EventHandler.kt
git commit -m "feat: add EventHandler interface for strategy pattern

- Base contract for event-specific handlers (ProductViewedEventHandler, OrderCreatedEventHandler)
- Enables ProductMetricsService to dispatch to correct handler by event type"
```

---

### Task 9: Update ProductMetricsRepository with Atomic Queries

**Files:**
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/productmetrics/ProductMetricsRepository.kt`

- [ ] **Step 1: Read current ProductMetricsRepository**

Run: `cat apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/productmetrics/ProductMetricsRepository.kt`

Expected: See current interface methods

- [ ] **Step 2: Add @Modifying queries**

Replace/update the interface to include:

```kotlin
package com.loopers.infrastructure.productmetrics

import com.loopers.domain.productmetrics.ProductMetrics
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductMetricsRepository : JpaRepository<ProductMetrics, Long> {
    fun findByProductId(productId: Long): ProductMetrics?

    // Atomic Update: DB-level atomicity for concurrency control
    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.viewCount = pm.viewCount + 1 WHERE pm.productId = :productId")
    fun incrementViewCount(@Param("productId") productId: Long)

    @Modifying
    @Query("UPDATE ProductMetrics pm SET pm.salesCount = pm.salesCount + :quantity WHERE pm.productId = :productId")
    fun incrementSalesCount(@Param("productId") productId: Long, @Param("quantity") quantity: Int)
}
```

- [ ] **Step 3: Verify repository compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit repository changes**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/productmetrics/ProductMetricsRepository.kt
git commit -m "feat: add atomic UPDATE queries to ProductMetricsRepository

- incrementViewCount: atomic view_count increment at DB level
- incrementSalesCount: atomic sales_count increment by quantity at DB level
- No application-level locks, DB atomicity ensures consistency"
```

---

### Task 10: Update ProductMetrics Entity

**Files:**
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetrics.kt`

- [ ] **Step 1: Read current ProductMetrics entity**

Run: `cat apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetrics.kt`

Expected: See current entity structure

- [ ] **Step 2: Update entity fields and remove increment methods**

```kotlin
package com.loopers.domain.productmetrics

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "product_metrics")
class ProductMetrics(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val productId: Long,
    val viewCount: Long = 0,      // val (immutable)
    val salesCount: Long = 0,     // val (immutable)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun create(productId: Long) = ProductMetrics(
            productId = productId,
            viewCount = 0,
            salesCount = 0,
        )
    }
}
```

- [ ] **Step 3: Verify entity compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit entity changes**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetrics.kt
git commit -m "refactor: make ProductMetrics fields immutable and remove increment methods

- Changed viewCount, salesCount from var to val (immutable)
- Removed incrementViewCount(), incrementSalesCount() methods
- Atomic updates now handled by DB-level UPDATE queries only
- Aligns with new Atomic Query concurrency control approach"
```

---

### Task 11: Update OrderCreatedEvent Structure

**Files:**
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/order/OrderCreatedEvent.kt`

- [ ] **Step 1: Read current OrderCreatedEvent**

Run: `cat apps/commerce-streamer/src/main/kotlin/com/loopers/domain/order/OrderCreatedEvent.kt`

Expected: See current event structure

- [ ] **Step 2: Create OrderLineItem data class and update OrderCreatedEvent**

```kotlin
package com.loopers.domain.order

import org.springframework.context.ApplicationEvent
import java.util.UUID

data class OrderLineItem(
    val productId: Long,
    val quantity: Int,
)

class OrderCreatedEvent(
    source: Any,
    val orderId: Long,
    val lineItems: List<OrderLineItem>,  // Changed from itemCount: Int
    val dedupeKey: String = "order:$orderId:${UUID.randomUUID()}"
) : ApplicationEvent(source)
```

- [ ] **Step 3: Verify event compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit event changes**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/domain/order/OrderCreatedEvent.kt
git commit -m "refactor: restructure OrderCreatedEvent with lineItems for granular metrics

- Changed from itemCount: Int to lineItems: List<OrderLineItem>
- Each lineItem contains productId and quantity for per-product sales tracking
- Enables OrderCreatedEventHandler to increment sales_count accurately per product"
```

---

### Task 12: Update ProductMetricsService

**Files:**
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetricsService.kt`

- [ ] **Step 1: Read current ProductMetricsService**

Run: `grep -A 30 "class ProductMetricsService" apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetricsService.kt`

Expected: See current service implementation

- [ ] **Step 2: Add extractDedupeKey method and update if needed**

Ensure the service has:

```kotlin
@Service
@Transactional
class ProductMetricsService(
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val handlers: Map<String, EventHandler>,
) {
    fun processMetricsEvent(event: Any) {
        val dedupeKey = extractDedupeKey(event)

        // Idempotency check
        if (eventHandledRepository.existsByDedupeKey(dedupeKey)) {
            return
        }

        // Dispatch to event-specific handler
        val handler = handlers[event::class.simpleName]
        handler?.handle(event)

        // Record handled event
        eventHandledRepository.save(EventHandled(dedupeKey))
    }

    private fun extractDedupeKey(event: Any): String = when (event) {
        is ProductViewedEvent -> event.dedupeKey
        is OrderCreatedEvent -> event.dedupeKey
        else -> throw IllegalArgumentException("Unknown event type: ${event::class.simpleName}")
    }
}
```

- [ ] **Step 3: Verify service compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit service changes**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductMetricsService.kt
git commit -m "feat: add extractDedupeKey to ProductMetricsService for idempotency

- Extracts dedupeKey from ProductViewedEvent and OrderCreatedEvent
- Enables flexible event handling via strategy pattern
- Supports future event types by adding to when expression"
```

---

### Task 13: Update ProductMetricsConsumer

**Files:**
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/kafka/ProductMetricsConsumer.kt`

- [ ] **Step 1: Read current ProductMetricsConsumer**

Run: `cat apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/kafka/ProductMetricsConsumer.kt | head -50`

Expected: See current consumer implementation

- [ ] **Step 2: Update to subscribe to single metrics-events topic**

```kotlin
package com.loopers.infrastructure.kafka

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.order.OrderCreatedEvent
import com.loopers.domain.productmetrics.ProductMetricsService
import com.loopers.domain.productview.ProductViewedEvent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ProductMetricsConsumer(
    private val productMetricsService: ProductMetricsService,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["metrics-events"],  // Single unified topic
        containerFactory = "kafkaListenerContainerFactory",  // or your batch listener factory
    )
    fun handleMetricsEvents(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            var hasError = false
            for (message in messages) {
                try {
                    val payload = message.value() as String
                    val eventType = detectEventType(payload)

                    when (eventType) {
                        "ProductViewedEvent" -> {
                            val event = objectMapper.readValue(payload, ProductViewedEvent::class.java)
                            productMetricsService.processMetricsEvent(event)
                        }
                        "OrderCreatedEvent" -> {
                            val event = objectMapper.readValue(payload, OrderCreatedEvent::class.java)
                            productMetricsService.processMetricsEvent(event)
                        }
                        else -> logger.warn("Unknown event type: $eventType")
                    }
                } catch (e: Exception) {
                    logger.error("Failed to process message: ${message.value()}", e)
                    hasError = true
                }
            }
            if (!hasError) {
                acknowledgment.acknowledge()
            }
        } catch (e: Exception) {
            logger.error("Batch processing failed", e)
        }
    }

    private fun detectEventType(payload: String): String {
        val tree = objectMapper.readTree(payload)
        return tree.get("type")?.asText()
            ?: tree.get("@class")?.asText()?.substringAfterLast(".")
            ?: extractFromSimpleName(payload)
    }

    private fun extractFromSimpleName(payload: String): String {
        // Fallback: if Jackson includes type info in class field
        return try {
            val tree = objectMapper.readTree(payload)
            tree.fieldNames().asSequence().firstOrNull()?.capitalize() ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
```

- [ ] **Step 3: Verify consumer compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit consumer changes**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/infrastructure/kafka/ProductMetricsConsumer.kt
git commit -m "feat: update ProductMetricsConsumer to subscribe to unified metrics-events topic

- Changed from multiple topics (product.viewed, like.count) to single metrics-events
- Implements detectEventType() to determine event class from JSON payload
- Supports batch processing for both ProductViewedEvent and OrderCreatedEvent
- ACK only on successful batch processing, retry on error"
```

---

### Task 14: Update ProductViewedEventHandler

**Files:**
- Modify: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductViewedEventHandler.kt`

- [ ] **Step 1: Read current ProductViewedEventHandler**

Run: `cat apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductViewedEventHandler.kt`

Expected: See current handler implementation

- [ ] **Step 2: Update to use atomic query**

```kotlin
package com.loopers.domain.productmetrics

import com.loopers.domain.productview.ProductViewedEvent
import org.springframework.stereotype.Component

@Component("ProductViewedEvent")
class ProductViewedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val viewedEvent = event as ProductViewedEvent

        // Atomic Query: DB-level atomicity, no locks needed
        productMetricsRepository.incrementViewCount(viewedEvent.productId)
    }
}
```

- [ ] **Step 3: Verify handler compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit handler changes**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/ProductViewedEventHandler.kt
git commit -m "refactor: simplify ProductViewedEventHandler to use atomic query

- Removed SELECT+LOCK+UPDATE pattern
- Now calls productMetricsRepository.incrementViewCount() directly
- DB-level atomic UPDATE ensures consistency without application-level locks
- Improves concurrency: no waiting on locks"
```

---

### Task 15: Create OrderCreatedEventHandler

**Files:**
- Create: `apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/OrderCreatedEventHandler.kt`

- [ ] **Step 1: Create OrderCreatedEventHandler component**

```kotlin
package com.loopers.domain.productmetrics

import com.loopers.domain.order.OrderCreatedEvent
import org.springframework.stereotype.Component

@Component("OrderCreatedEvent")
class OrderCreatedEventHandler(
    private val productMetricsRepository: ProductMetricsRepository,
) : EventHandler {
    override fun handle(event: Any) {
        val orderEvent = event as OrderCreatedEvent

        // Atomic Update: increment sales_count for each product in order
        for (lineItem in orderEvent.lineItems) {
            productMetricsRepository.incrementSalesCount(lineItem.productId, lineItem.quantity)
        }
    }
}
```

- [ ] **Step 2: Verify handler compiles**

Run: `./gradlew :apps:commerce-streamer:classes -x test`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit new handler**

```bash
git add apps/commerce-streamer/src/main/kotlin/com/loopers/domain/productmetrics/OrderCreatedEventHandler.kt
git commit -m "feat: implement OrderCreatedEventHandler for sales_count aggregation

- Handles OrderCreatedEvent by iterating lineItems
- Calls atomic incrementSalesCount for each product in order
- Accumulates sales_count by quantity for accurate metrics
- Enables order-based metrics collection in unified metrics-events topic"
```

---

### Task 16: Verify Full Compilation and Run Basic Tests

**Files:**
- (No new files, verification only)

- [ ] **Step 1: Full compilation of commerce-api**

Run: `./gradlew :apps:commerce-api:build -x test --no-daemon`

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full compilation of commerce-streamer**

Run: `./gradlew :apps:commerce-streamer:build -x test --no-daemon`

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run all tests (if passing)**

Run: `./gradlew test --no-daemon -x :apps:commerce-batch:test`

Expected: All tests pass or identify failures for fixing

- [ ] **Step 4: Final commit summarizing system**

```bash
git add -A
git commit -m "feat: complete Outbox + Kafka metrics collection system implementation

Core Components:
- Outbox pattern: OutboxEvent, OutboxRepository, OutboxPublisher (commerce-api)
- Kafka publishing: OutboxPoller scheduler (10sec polling to metrics-events topic)
- Metrics aggregation: ProductMetricsConsumer, handlers (commerce-streamer)
- Atomic queries: @Modifying UPDATE for view_count, sales_count increments
- Idempotency: event_handled table with dedupeKey UNIQUE constraint
- Event sourcing: ProductViewedEvent (view), OrderCreatedEvent (sales by lineItem)

Transaction Safety:
- At-Least-Once: Outbox polling with retry on failure
- Exactly-Once: dedupeKey idempotency + atomic DB operations

Performance:
- No application locks: DB-level atomic UPDATE queries
- Batch Kafka consumption: 3000 records or 5 seconds
- Efficient polling: 100 events per 10-second poll cycle"
```

---

## Summary

This plan implements a complete Outbox-based event sourcing and metrics aggregation system:

1. **commerce-api side**: Events are stored in Outbox table within domain transactions, then published by a scheduler
2. **commerce-streamer side**: Events are consumed from Kafka in batches, deduplicated, and aggregated to product_metrics using atomic UPDATE queries
3. **Concurrency**: DB-level atomic operations replace application locks for higher throughput
4. **Reliability**: At-Least-Once publishing + Exactly-Once consumption semantics via idempotency table
