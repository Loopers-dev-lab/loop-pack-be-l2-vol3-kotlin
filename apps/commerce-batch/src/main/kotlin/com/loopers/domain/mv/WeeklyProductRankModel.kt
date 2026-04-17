package com.loopers.domain.mv

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 주간 랭킹 Materialized View 엔티티 (배치 **쓰기 전용**).
 *
 * - `period_start` 는 해당 주의 월요일, `period_end` 는 일요일 (KST 기준).
 * - 같은 `period_start` 안에서 `product_id` 와 `rank_position` 은 각각 유일하다.
 * - 배치 purge Step 이 `period_start` 단위로 선제 삭제 후 chunk Step 이 TOP 100 을 재삽입한다.
 */
@Entity
@Table(
    name = "mv_product_rank_weekly",
    indexes = [
        Index(name = "idx_mpw_start_rank", columnList = "period_start, rank_position"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_mpw_start_product", columnNames = ["period_start", "product_id"]),
        UniqueConstraint(name = "uk_mpw_start_rank", columnNames = ["period_start", "rank_position"]),
    ],
)
class WeeklyProductRankModel(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    rankPosition: Int,
    productId: Long,
    likesCount: Long,
    viewsCount: Long,
    salesCount: Long,
    score: Double,
) : BaseEntity() {
    @Column(name = "period_start", nullable = false)
    var periodStart: LocalDate = periodStart
        protected set

    @Column(name = "period_end", nullable = false)
    var periodEnd: LocalDate = periodEnd
        protected set

    @Column(name = "rank_position", nullable = false)
    var rankPosition: Int = rankPosition
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "likes_count", nullable = false)
    var likesCount: Long = likesCount
        protected set

    @Column(name = "views_count", nullable = false)
    var viewsCount: Long = viewsCount
        protected set

    @Column(name = "sales_count", nullable = false)
    var salesCount: Long = salesCount
        protected set

    @Column(name = "score", nullable = false)
    var score: Double = score
        protected set
}
