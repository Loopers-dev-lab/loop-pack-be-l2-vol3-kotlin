package com.loopers.domain.metrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * product_metrics 테이블 DDL 생성 및 랭킹 DB fallback 조회를 위한 엔티티.
 */
@Entity
@Table(name = "product_metrics")
class ProductMetrics(
    @Id
    val productId: Long = 0,

    @Column(nullable = false)
    var likeCount: Long = 0,

    @Column(nullable = false)
    var salesCount: Long = 0,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @Column(nullable = false)
    var version: Long = 0,

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
)
