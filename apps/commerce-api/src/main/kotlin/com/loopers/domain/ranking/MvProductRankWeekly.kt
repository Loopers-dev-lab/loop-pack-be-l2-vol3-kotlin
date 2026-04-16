package com.loopers.domain.ranking

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "mv_product_rank_weekly")
class MvProductRankWeekly(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    val productId: Long,
    @Column(name = "rank_value")
    val rank: Int,
    val score: Double,
    // e.g., "2026-W15"
    val yearWeek: String,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
