package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "staging_product_rank_monthly")
@IdClass(MonthlyRankId::class)
class StagingProductRankMonthly(
    @Id
    @Column(name = "`year_month`", length = 7, nullable = false)
    val yearMonth: String,

    @Id
    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(nullable = false)
    var rankNum: Int = 0,

    @Column(nullable = false, columnDefinition = "DOUBLE")
    var score: Double = 0.0,

    @Column(nullable = false)
    var viewCount: Long = 0,

    @Column(nullable = false)
    var likeCount: Long = 0,

    @Column(nullable = false)
    var salesCount: Long = 0,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
