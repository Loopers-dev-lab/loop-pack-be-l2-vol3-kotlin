package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankMvRepository
import com.loopers.domain.ranking.ProductRankMvRow
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ProductRankMvJdbcRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ProductRankMvRepository {

    override fun findWeeklyByPeriodStartDate(periodStartDate: LocalDate): List<ProductRankMvRow> =
        queryMv(WEEKLY_QUERY, periodStartDate)

    override fun findMonthlyByPeriodStartDate(periodStartDate: LocalDate): List<ProductRankMvRow> =
        queryMv(MONTHLY_QUERY, periodStartDate)

    private fun queryMv(sql: String, periodStartDate: LocalDate): List<ProductRankMvRow> {
        val params = MapSqlParameterSource("periodStartDate", periodStartDate)
        return jdbcTemplate.query(sql, params) { rs, _ ->
            ProductRankMvRow(
                productId = rs.getLong("product_id"),
                totalScore = rs.getDouble("total_score"),
                viewCount = rs.getInt("view_count"),
                likeCount = rs.getInt("like_count"),
                orderCount = rs.getInt("order_count"),
                rank = rs.getInt("rank"),
            )
        }
    }

    companion object {
        private const val WEEKLY_QUERY = """
            SELECT product_id, total_score, view_count, like_count, order_count, `rank`
            FROM mv_product_rank_weekly
            WHERE period_start_date = :periodStartDate
            ORDER BY `rank`
        """

        private const val MONTHLY_QUERY = """
            SELECT product_id, total_score, view_count, like_count, order_count, `rank`
            FROM mv_product_rank_monthly
            WHERE period_start_date = :periodStartDate
            ORDER BY `rank`
        """
    }
}
