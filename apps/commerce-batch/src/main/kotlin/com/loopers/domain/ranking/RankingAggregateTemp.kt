package com.loopers.domain.ranking

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Comment

/**
 * 랭킹 집계 중간 테이블.
 *
 * Step 1(Chunk)에서 product_metrics 기간 합산 + score 계산 결과를 저장한다.
 * Step 2(Tasklet)에서 score DESC 정렬 후 TOP 100을 MV에 적재하고 이 테이블을 cleanup한다.
 */
@Entity
@Table(name = "ranking_aggregate_temp")
@Comment("랭킹 집계 중간 테이블")
class RankingAggregateTemp(
    productId: Long,
    score: Double,
    likeCount: Long,
    orderCount: Long,
    viewCount: Long,
    periodType: String,
    periodKey: String,
) : BaseEntity() {

    @Comment("상품 ID")
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Comment("가중합산 스코어")
    @Column(name = "score", nullable = false)
    var score: Double = score
        protected set

    @Comment("좋아요 합계")
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = likeCount
        protected set

    @Comment("주문 합계")
    @Column(name = "order_count", nullable = false)
    var orderCount: Long = orderCount
        protected set

    @Comment("조회 합계")
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = viewCount
        protected set

    @Comment("기간 유형 (WEEKLY / MONTHLY)")
    @Column(name = "period_type", nullable = false, length = 10)
    var periodType: String = periodType
        protected set

    @Comment("기간 키 (2026-W15 / 2026-04)")
    @Column(name = "period_key", nullable = false, length = 10)
    var periodKey: String = periodKey
        protected set
}
