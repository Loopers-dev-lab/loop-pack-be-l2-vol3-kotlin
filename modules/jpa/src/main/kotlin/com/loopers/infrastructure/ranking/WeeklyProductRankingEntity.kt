package com.loopers.infrastructure.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "mv_product_rank_weekly",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_mv_product_rank_weekly_week_start_product_id",
            columnNames = ["week_start_date", "product_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_mv_product_rank_weekly_week_start_ranking", columnList = "week_start_date, ranking"),
    ],
)
class WeeklyProductRankingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "week_start_date", nullable = false)
    val weekStartDate: LocalDate,

    @Column(name = "week_end_date", nullable = false)
    val weekEndDate: LocalDate,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "ranking", nullable = false)
    val ranking: Long,

    @Column(name = "score", nullable = false)
    val score: Double,

    @Column(name = "like_count", nullable = false)
    val likeCount: Long,

    @Column(name = "view_count", nullable = false)
    val viewCount: Long,

    @Column(name = "sales_count", nullable = false)
    val salesCount: Long,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: ZonedDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: ZonedDateTime? = null,
) {
    @PrePersist
    fun prePersist() {
        val now = ZonedDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}
