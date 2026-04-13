package com.loopers.batch.ranking.entity

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "mv_product_rank_weekly",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["period_key", "product_id"]),
        UniqueConstraint(columnNames = ["period_key", "rank_no"]),
    ],
)
class MvProductRankWeeklyBatchEntity(
    @Column(name = "rank_no") val rankNo: Int,
    @Column(name = "product_id") val productId: Long,
    @Column(name = "score") val score: Double,
    @Column(name = "view_count") val viewCount: Long,
    @Column(name = "like_count") val likeCount: Long,
    @Column(name = "sales_count") val salesCount: Long,
    @Column(name = "period_key") val periodKey: String,
    @Column(name = "period_start_date") val periodStartDate: LocalDate,
    @Column(name = "period_end_date") val periodEndDate: LocalDate,
) : BaseEntity()
