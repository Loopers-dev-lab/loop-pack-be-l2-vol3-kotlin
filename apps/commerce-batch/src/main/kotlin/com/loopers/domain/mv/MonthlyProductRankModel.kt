package com.loopers.domain.mv

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

/**
 * 월간 랭킹 Materialized View 엔티티 (배치 **쓰기 전용**).
 *
 * - `year_month_val` 은 `yyyy-MM` 포맷 (예: `2026-04`). `year_month` 는 MySQL 예약어 가능성이 있어 `_val` 접미사를 붙였다.
 * - `period_start` 는 해당 월 1일, `period_end` 는 말일.
 * - 같은 `year_month_val` 안에서 `product_id` 와 `rank_position` 은 각각 유일하다.
 */
@Entity
@Table(
    name = "mv_product_rank_monthly",
    indexes = [
        Index(name = "idx_mpm_month_rank", columnList = "year_month_val, rank_position"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_mpm_month_product", columnNames = ["year_month_val", "product_id"]),
        UniqueConstraint(name = "uk_mpm_month_rank", columnNames = ["year_month_val", "rank_position"]),
    ],
)
class MonthlyProductRankModel(
    yearMonthVal: String,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    rankPosition: Int,
    productId: Long,
    likesCount: Long,
    viewsCount: Long,
    salesCount: Long,
    score: Double,
) : BaseEntity() {
    @Column(name = "year_month_val", nullable = false, length = 7)
    var yearMonthVal: String = yearMonthVal
        protected set

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
