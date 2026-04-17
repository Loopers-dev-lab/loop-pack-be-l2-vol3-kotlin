package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "mv_product_rank_weekly",
    indexes = [
        Index(name = "idx_mv_weekly_period_rank", columnList = "period_start_date, rank"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_mv_weekly_product_period", columnNames = ["product_id", "period_start_date"]),
    ],
)
class ProductRankWeekly(
    productId: Long,
    totalScore: Double,
    viewCount: Int,
    likeCount: Int,
    orderCount: Int,
    rank: Int,
    periodStartDate: LocalDate,
    periodEndDate: LocalDate,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @Column(name = "product_id", nullable = false)
    val productId: Long = productId

    @Column(name = "total_score", nullable = false)
    val totalScore: Double = totalScore

    @Column(name = "view_count", nullable = false)
    val viewCount: Int = viewCount

    @Column(name = "like_count", nullable = false)
    val likeCount: Int = likeCount

    @Column(name = "order_count", nullable = false)
    val orderCount: Int = orderCount

    @Column(name = "rank", nullable = false)
    val rank: Int = rank

    @Column(name = "period_start_date", nullable = false)
    val periodStartDate: LocalDate = periodStartDate

    @Column(name = "period_end_date", nullable = false)
    val periodEndDate: LocalDate = periodEndDate

    @Column(name = "created_at", nullable = false, updatable = false)
    lateinit var createdAt: ZonedDateTime
        private set

    @PrePersist
    private fun prePersist() {
        createdAt = ZonedDateTime.now()
    }
}
