package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDateTime

/**
 * 주간 상품 랭킹 Materialized View.
 *
 * 배치 Job이 product_metrics를 주간 합산하여 TOP 100을 적재한다.
 * version 컬럼으로 블루-그린 교체: 새 version INSERT → 이전 version 삭제.
 */
@Entity
@Table(
    name = "mv_product_rank_weekly",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "year_week", "data_version"])],
)
@Comment("주간 상품 랭킹 MV")
class MvProductRankWeekly(
    productId: Long,
    yearWeek: String,
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

    @Comment("ISO 주차 - 예 2026-W15")
    @Column(name = "year_week", nullable = false, length = 10)
    var yearWeek: String = yearWeek
        protected set

    @Comment("순위 1-100")
    @Column(name = "ranking_rank", nullable = false)
    var rank: Int = rank
        protected set

    @Comment("가중합산 스코어")
    @Column(name = "score", nullable = false)
    var score: Double = score
        protected set

    @Comment("주간 좋아요 합계")
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Comment("주간 주문 합계")
    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    @Comment("주간 조회 합계")
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
