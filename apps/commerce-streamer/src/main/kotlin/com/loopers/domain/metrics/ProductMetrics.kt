package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.persistence.Version
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 상품 메트릭 집계 테이블.
 *
 * commerce-streamer의 MetricsConsumer가 Kafka 이벤트를 소비하여 upsert한다.
 * 좋아요 수, 주문 수, 조회 수를 상품별 + 일별로 집계한다.
 *
 * 9주차: 상품당 1행 (누적) — 실시간 갱신에 최적화
 * 10주차: 상품+날짜별 1행 — 배치에서 기간별 집계 가능하도록 변경
 */
@Entity
@Table(
    name = "product_metrics",
    uniqueConstraints = [UniqueConstraint(columnNames = ["product_id", "metric_date"])],
)
@Comment("상품 메트릭 일별 집계")
class ProductMetrics(
    productId: Long,
    metricDate: LocalDate,
) : BaseEntity() {

    @Comment("상품 ID")
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Comment("집계 날짜")
    @Column(name = "metric_date", nullable = false)
    var metricDate: LocalDate = metricDate
        protected set

    @Comment("좋아요 수")
    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0
        protected set

    @Comment("주문 수 (주문 내 해당 상품 건수)")
    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0
        protected set

    @Comment("조회 수")
    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0
        protected set

    @Comment("마지막 처리된 이벤트 시점")
    @Column(name = "last_event_at")
    var lastEventAt: ZonedDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    /**
     * 이벤트 시점이 마지막 처리 시점보다 오래되었으면 true.
     * 상태 덮어쓰기 방식이 아닌 증감 방식에서는 실질적으로 발생하지 않지만,
     * 향후 snapshot 방식 전환 시 안전장치로 동작한다.
     */
    fun isStaleEvent(eventOccurredAt: ZonedDateTime): Boolean {
        val last = lastEventAt ?: return false
        return eventOccurredAt.isBefore(last)
    }

    fun updateLastEventAt(eventOccurredAt: ZonedDateTime) {
        this.lastEventAt = eventOccurredAt
    }

    fun incrementLikeCount() {
        this.likeCount++
    }

    fun decrementLikeCount() {
        if (this.likeCount > 0) this.likeCount--
    }

    fun incrementOrderCount(count: Long = 1) {
        this.orderCount += count
    }

    fun incrementViewCount() {
        this.viewCount++
    }
}
