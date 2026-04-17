package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "mv_product_rank_weekly",
    indexes = [Index(name = "idx_mvrw_week_end", columnList = "week_end, rank_position")],
)
class WeeklyRank private constructor(
    id: WeeklyRankId,
    weekStart: LocalDate,
    viewCount: Long,
    likeCount: Long,
    orderCount: Long,
    totalScore: Double,
    rankPosition: Int,
) {

    @EmbeddedId
    val id: WeeklyRankId = id

    @Column(name = "week_start", nullable = false)
    var weekStart: LocalDate = weekStart
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    @Column(name = "total_score", nullable = false)
    var totalScore: Double = totalScore
        protected set

    @Column(name = "rank_position", nullable = false)
    var rankPosition: Int = rankPosition
        protected set

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: ZonedDateTime
        protected set

    @PrePersist
    private fun prePersist() {
        updatedAt = ZonedDateTime.now()
    }

    @PreUpdate
    private fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }

    val productId: Long get() = id.productId
    val weekEnd: LocalDate get() = id.weekEnd

    companion object {
        fun create(
            productId: Long,
            weekStart: LocalDate,
            weekEnd: LocalDate,
            rankPosition: Int,
            totalScore: Double,
            viewCount: Long = 0L,
            likeCount: Long = 0L,
            orderCount: Long = 0L,
        ): WeeklyRank = WeeklyRank(
            id = WeeklyRankId(productId, weekEnd),
            weekStart = weekStart,
            viewCount = viewCount,
            likeCount = likeCount,
            orderCount = orderCount,
            totalScore = totalScore,
            rankPosition = rankPosition,
        )
    }
}
