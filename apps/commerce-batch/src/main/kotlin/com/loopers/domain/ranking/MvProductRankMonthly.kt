package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

/**
 * 월간 상품 랭킹 Materialized View.
 *
 * 배치 Job이 product_metrics를 월간 합산하여 TOP 100을 적재한다.
 * version 컬럼으로 블루-그린 교체: 새 version INSERT → 이전 version 삭제.
 */
@Entity
@Table(
    name = "mv_product_rank_monthly",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "period_month", "data_version"])],
)
@Comment("월간 상품 랭킹 MV")
class MvProductRankMonthly(
    productId: Long,
    yearMonth: String,
    rank: Int,
    score: Double,
    likeCount: Long,
    orderCount: Long,
    viewCount: Long,
    version: Int,
    aggregatedAt: LocalDateTime,
) : BaseEntity() {

    @Comment("상품 ID")
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Comment("캘린더 월 - 예 2026-04")
    @Column(name = "period_month", nullable = false, length = 7)
    var yearMonth: String = yearMonth
        protected set

    @Comment("순위 1-100")
    @Column(name = "ranking_rank", nullable = false)
    var rank: Int = rank
        protected set

    @Comment("가중합산 스코어")
    @Column(name = "score", nullable = false)
    var score: Double = score
        protected set

    @Comment("월간 좋아요 합계")
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Comment("월간 주문 합계")
    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    @Comment("월간 조회 합계")
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Comment("블루-그린 교체용 버전")
    @Column(name = "data_version", nullable = false)
    var version: Int = version
        protected set

    @Comment("집계 시점")
    @Column(name = "aggregated_at", nullable = false)
    var aggregatedAt: LocalDateTime = aggregatedAt
        protected set
}
