package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

data class MonthlyRankId(
    val yearMonth: String = "",
    val productId: Long = 0,
) : Serializable

@Entity
@Table(name = "mv_product_rank_monthly")
@IdClass(MonthlyRankId::class)
class MvProductRankMonthly(
    @Id
    @Column(name = "`year_month`", length = 7, nullable = false)
    val yearMonth: String = "",

    @Id
    @Column(name = "product_id", nullable = false)
    val productId: Long = 0,

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
