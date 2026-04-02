package com.loopers.domain.metrics

import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "product_metrics",
    indexes = [
        Index(name = "idx_product_metrics_updated_at", columnList = "updated_at"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_product_metrics_product_id", columnNames = ["product_id"]),
    ],
)
class ProductMetricsModel(
    productId: Long,
) : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "likes_count", nullable = false)
    var likesCount: Long = 0
        protected set

    @Column(name = "views_count", nullable = false)
    var viewsCount: Long = 0
        protected set

    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = 0
        protected set

    @Column(name = "last_event_version", nullable = false)
    var lastEventVersion: Long = 0
        protected set

    @Column(name = "last_event_at", nullable = false)
    var lastEventAt: ZonedDateTime = ZonedDateTime.parse("1970-01-01T00:00:00Z")
        protected set

    fun isStale(event: CatalogEventMessage): Boolean {
        return when {
            event.version < lastEventVersion -> true
            event.version > lastEventVersion -> false
            else -> event.occurredAt.isBefore(lastEventAt) || event.occurredAt.isEqual(lastEventAt)
        }
    }

    fun apply(event: CatalogEventMessage) {
        when (event.eventType) {
            CatalogEventType.LIKE_CHANGED -> likesCount += event.delta
            CatalogEventType.PRODUCT_VIEWED -> viewsCount += event.delta
        }
        lastEventVersion = event.version
        lastEventAt = event.occurredAt
    }
}
