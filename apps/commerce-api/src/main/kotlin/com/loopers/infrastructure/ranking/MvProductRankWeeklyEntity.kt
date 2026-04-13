package com.loopers.infrastructure.ranking

import com.loopers.domain.BaseEntity
import com.loopers.domain.ranking.model.WeeklyProductRank
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
class MvProductRankWeeklyEntity(
    @Column(name = "rank_no", nullable = false)
    val rankNo: Int,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "score", nullable = false)
    val score: Double,
    @Column(name = "view_count", nullable = false)
    val viewCount: Long,
    @Column(name = "like_count", nullable = false)
    val likeCount: Long,
    @Column(name = "sales_count", nullable = false)
    val salesCount: Long,
    @Column(name = "period_key", nullable = false)
    val periodKey: String,
    @Column(name = "period_start_date", nullable = false)
    val periodStartDate: LocalDate,
    @Column(name = "period_end_date", nullable = false)
    val periodEndDate: LocalDate,
) : BaseEntity() {

    fun toDomain(): WeeklyProductRank = WeeklyProductRank(
        rank = rankNo,
        productId = productId,
        score = score,
        viewCount = viewCount,
        likeCount = likeCount,
        salesCount = salesCount,
        periodKey = periodKey,
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
    )
}
