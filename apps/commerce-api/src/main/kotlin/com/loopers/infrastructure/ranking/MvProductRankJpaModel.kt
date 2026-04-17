package com.loopers.infrastructure.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.io.Serializable
import java.time.ZonedDateTime

data class MvProductRankId(
    val periodKey: String = "",
    val productId: Long = 0L,
) : Serializable

@Entity
@Table(
    name = "mv_product_rank_weekly",
    indexes = [Index(name = "idx_mv_product_rank_weekly_rank", columnList = "period_key, rank_value")],
)
@IdClass(MvProductRankId::class)
class MvProductRankWeeklyJpaModel(
    @Id
    @Column(name = "period_key", length = 16, nullable = false)
    var periodKey: String,

    @Id
    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(name = "rank_value", nullable = false)
    var rankValue: Int,

    @Column(nullable = false)
    var score: Double,

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0L,

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L,

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0L,

    @Column(name = "order_amount_sum", nullable = false)
    var orderAmountSum: Long = 0L,

    @Column(name = "computed_at", nullable = false)
    var computedAt: ZonedDateTime,
)

@Entity
@Table(
    name = "mv_product_rank_monthly",
    indexes = [Index(name = "idx_mv_product_rank_monthly_rank", columnList = "period_key, rank_value")],
)
@IdClass(MvProductRankId::class)
class MvProductRankMonthlyJpaModel(
    @Id
    @Column(name = "period_key", length = 16, nullable = false)
    var periodKey: String,

    @Id
    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(name = "rank_value", nullable = false)
    var rankValue: Int,

    @Column(nullable = false)
    var score: Double,

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0L,

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L,

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0L,

    @Column(name = "order_amount_sum", nullable = false)
    var orderAmountSum: Long = 0L,

    @Column(name = "computed_at", nullable = false)
    var computedAt: ZonedDateTime,
)
