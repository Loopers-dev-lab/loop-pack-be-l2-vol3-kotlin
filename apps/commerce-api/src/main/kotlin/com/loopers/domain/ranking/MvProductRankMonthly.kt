package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

/**
 * 월간 상품 랭킹 MV (조회 전용).
 *
 * commerce-batch에서 적재한 데이터를 commerce-api에서 읽기 위한 엔티티.
 */
@Entity
@Table(name = "mv_product_rank_monthly")
@Comment("월간 상품 랭킹 MV")
class MvProductRankMonthly : BaseEntity() {

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0
        protected set

    @Column(name = "period_month", nullable = false)
    var yearMonth: String = ""
        protected set

    @Column(name = "ranking_rank", nullable = false)
    var rank: Int = 0
        protected set

    @Column(name = "score", nullable = false)
    var score: Double = 0.0
        protected set

    @Column(name = "data_version", nullable = false)
    var version: Int = 0
        protected set
}
