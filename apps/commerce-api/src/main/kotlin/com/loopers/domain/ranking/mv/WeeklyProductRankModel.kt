package com.loopers.domain.ranking.mv

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

/**
 * 주간 랭킹 Materialized View 엔티티 (API **읽기 전용**).
 *
 * 배치 모듈이 적재하는 `mv_product_rank_weekly` 테이블을 API 측에서 조회할 때 사용한다.
 * `@Immutable` 로 Hibernate 가 UPDATE 를 발행하지 않도록 방어한다.
 */
@Entity
@Immutable
@Table(name = "mv_product_rank_weekly")
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
