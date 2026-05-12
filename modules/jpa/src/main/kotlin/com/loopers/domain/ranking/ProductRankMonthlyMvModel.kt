package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "tb_mv_product_rank_monthly",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["period_start_date", "product_id"]),
    ],
)
class ProductRankMonthlyMvModel(
    periodStartDate: LocalDate,
    periodEndDate: LocalDate,
    rankPosition: Long,
    productId: Long,
    score: Double,
) : BaseEntity() {
    @Column(name = "period_start_date", nullable = false)
    var periodStartDate: LocalDate = periodStartDate
        protected set

    @Column(name = "period_end_date", nullable = false)
    var periodEndDate: LocalDate = periodEndDate
        protected set

    @Column(name = "rank_position", nullable = false)
    var rankPosition: Long = rankPosition
        protected set

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "score", nullable = false)
    var score: Double = score
        protected set
}
