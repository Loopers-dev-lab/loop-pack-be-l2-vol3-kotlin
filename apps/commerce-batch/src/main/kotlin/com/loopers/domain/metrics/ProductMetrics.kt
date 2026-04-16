package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Comment
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * product_metrics 엔티티 (commerce-batch 전용, 읽기 목적).
 *
 * commerce-streamer의 동명 엔티티와 동일한 테이블을 매핑하지만,
 * 모듈 독립성을 위해 별도 정의한다. Hibernate ddl-auto=create 시 테이블 생성에 사용.
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

    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "metric_date", nullable = false)
    var metricDate: LocalDate = metricDate
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0
        protected set

    @Column(name = "order_count", nullable = false)
    var orderCount: Long = 0
        protected set

    @Column(name = "view_count", nullable = false)
    var viewCount: Long = 0
        protected set

    @Column(name = "last_event_at")
    var lastEventAt: ZonedDateTime? = null
        protected set

    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set
}
