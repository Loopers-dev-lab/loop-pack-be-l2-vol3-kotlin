package com.loopers.infrastructure.ranking

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * 주간 랭킹 Materialized View 엔티티 (commerce-api 읽기 전용).
 *
 * 쓰기 주체는 commerce-batch의 WeeklyRankJob.
 * 스키마는 commerce-batch의 동명 엔티티와 1:1 동일하게 유지한다 (drift 방지).
 * `rank_num` 컬럼명은 MySQL 예약어 `RANK` 회피용. 도메인은 `rank`로 노출한다.
 */
@Table(
    name = "mv_product_rank_weekly",
    indexes = [
        Index(
            name = "idx_mv_product_rank_weekly_product_year_week",
            columnList = "product_id, year, week",
            unique = true,
        ),
        Index(
            name = "idx_mv_product_rank_weekly_year_week_rank",
            columnList = "year, week, rank_num",
        ),
    ],
)
@Entity
class WeeklyProductRankEntity(
    id: Long? = null,
    @Column(name = "product_id", nullable = false)
    val productId: Long,
    @Column(name = "year", nullable = false)
    val year: Int,
    @Column(name = "week", nullable = false)
    val week: Int,
    @Column(name = "total_score", nullable = false)
    val totalScore: Double,
    @Column(name = "rank_num", nullable = false)
    val rankNumber: Int,
    @Column(name = "view_count", nullable = false)
    val viewCount: Int,
    @Column(name = "like_count", nullable = false)
    val likeCount: Int,
    @Column(name = "units_sold", nullable = false)
    val unitsSold: Int,
    @Column(name = "sales_amount", nullable = false)
    val salesAmount: Long,
    @Column(name = "order_score", nullable = false)
    val orderScore: Double,
) : BaseEntity() {
    init {
        this.id = id
    }
}
