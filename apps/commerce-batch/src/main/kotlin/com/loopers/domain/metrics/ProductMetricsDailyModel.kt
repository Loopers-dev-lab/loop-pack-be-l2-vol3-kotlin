package com.loopers.domain.metrics

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Immutable
import java.time.LocalDate

/**
 * `product_metrics_daily` 의 배치 모듈 측 매핑 (**읽기 전용**).
 *
 * - streamer 모듈이 이벤트 소비 시 upsert 하는 일별 스냅샷을 배치가 읽는다.
 * - 배치 Reader 는 raw SQL(`JdbcCursorItemReader`) 로 직접 조회하므로 본 엔티티는
 *   1) `ddl-auto: create` 환경에서 테이블 DDL 생성용, 2) 테스트에서 `.save()` 로 시드 삽입용.
 * - 쓰기가 발생하지 않도록 `@Immutable` 로 방어한다.
 */
@Entity
@Immutable
@Table(
    name = "product_metrics_daily",
    indexes = [
        Index(name = "idx_pmd_metric_date", columnList = "metric_date"),
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_pmd_product_date", columnNames = ["product_id", "metric_date"]),
    ],
)
class ProductMetricsDailyModel(
    productId: Long,
    metricDate: LocalDate,
    likesCount: Long = 0,
    viewsCount: Long = 0,
    salesCount: Long = 0,
) : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "metric_date", nullable = false)
    var metricDate: LocalDate = metricDate
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
}
