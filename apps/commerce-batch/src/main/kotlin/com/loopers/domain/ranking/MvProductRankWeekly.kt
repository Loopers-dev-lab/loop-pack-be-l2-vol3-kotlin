package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

data class WeeklyRankId(
    val yearWeek: String = "",
    val productId: Long = 0,
) : Serializable

@Entity
@Table(name = "mv_product_rank_weekly")
@IdClass(WeeklyRankId::class)
class MvProductRankWeekly(
    @Id
    @Column(name = "year_week", length = 10, nullable = false)
    val yearWeek: String,

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
