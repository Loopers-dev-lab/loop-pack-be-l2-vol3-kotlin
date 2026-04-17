package com.loopers.infrastructure.ranking

import com.loopers.support.jpa.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "mv_product_rank_monthly",
    indexes = [
        Index(name = "idx_mv_monthly_date_ranking", columnList = "ranking_date, ranking"),
    ],
)
class ProductRankMonthlyEntity(
    id: Long?,

    @Column(name = "product_id", nullable = false)
    val productId: Long,

    @Column(name = "score", nullable = false)
    val score: Double,

    @Column(name = "ranking", nullable = false)
    val ranking: Int,

    @Column(name = "ranking_date", nullable = false)
    val rankingDate: LocalDate,
) : BaseEntity() {
    init {
        this.id = id
    }
}
