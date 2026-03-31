package com.loopers.infrastructure.metric

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "product_metrics")
@Entity
class ProductMetricEntity(
    id: Long? = null,
    @Column(name = "product_id", nullable = false, unique = true)
    val productId: Long,
    @Column(name = "view_count", nullable = false)
    var viewCount: Int = 0,
    @Column(name = "like_count", nullable = false)
    var likeCount: Int = 0,
    @Column(name = "units_sold", nullable = false)
    var unitsSold: Int = 0,
    @Column(name = "catalog_event_version", nullable = false)
    var catalogEventVersion: Long = 0L,
    @Column(name = "order_event_version", nullable = false)
    var orderEventVersion: Long = 0L,
) : BaseEntity() {
    init {
        this.id = id
    }
}
