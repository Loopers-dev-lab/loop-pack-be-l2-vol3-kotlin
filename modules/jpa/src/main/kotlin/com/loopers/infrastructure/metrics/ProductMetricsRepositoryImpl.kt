package com.loopers.infrastructure.metrics

import com.loopers.domain.metrics.ProductMetricsDeltaCommand
import com.loopers.domain.metrics.ProductMetricsModel
import com.loopers.domain.metrics.ProductMetricsRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ProductMetricsRepositoryImpl(
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) : ProductMetricsRepository {
    override fun upsertDailyMetrics(command: ProductMetricsDeltaCommand) {
        namedParameterJdbcTemplate.update(
            UPSERT_SQL,
            MapSqlParameterSource()
                .addValue("productId", command.productId)
                .addValue("metricsDate", command.metricsDate)
                .addValue("viewDelta", command.viewDelta)
                .addValue("likeDelta", command.likeDelta)
                .addValue("salesDelta", command.salesDelta)
                .addValue("scoreDelta", command.scoreDelta)
                .addValue("occurredAt", command.occurredAt)
                .addValue("recordedAt", command.recordedAt),
        )
    }

    override fun findByProductIdAndMetricsDate(productId: Long, metricsDate: LocalDate): ProductMetricsModel? =
        productMetricsJpaRepository.findByProductIdAndMetricsDate(productId, metricsDate)

    companion object {
        private const val UPSERT_SQL = """
            INSERT INTO tb_product_metrics (
                product_id,
                metrics_date,
                view_count,
                like_count,
                sales_count,
                ranking_score,
                last_event_at,
                created_at,
                updated_at
            ) VALUES (
                :productId,
                :metricsDate,
                :viewDelta,
                :likeDelta,
                :salesDelta,
                :scoreDelta,
                :occurredAt,
                :recordedAt,
                :recordedAt
            )
            ON DUPLICATE KEY UPDATE
                view_count = view_count + :viewDelta,
                like_count = GREATEST(0, like_count + :likeDelta),
                sales_count = sales_count + :salesDelta,
                ranking_score = ranking_score + :scoreDelta,
                last_event_at = CASE
                    WHEN last_event_at IS NULL OR last_event_at < :occurredAt THEN :occurredAt
                    ELSE last_event_at
                END,
                updated_at = :recordedAt
        """
    }
}
