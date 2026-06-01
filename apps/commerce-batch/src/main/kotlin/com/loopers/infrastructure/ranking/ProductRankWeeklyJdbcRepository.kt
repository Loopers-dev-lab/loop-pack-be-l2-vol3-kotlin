package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankResult
import com.loopers.domain.ranking.ProductRankWeeklyRepository
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class ProductRankWeeklyJdbcRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : ProductRankWeeklyRepository {

    override fun batchInsert(entities: List<ProductRankResult>, periodStartDate: LocalDate, periodEndDate: LocalDate) {
        val params = entities.map { result ->
            MapSqlParameterSource()
                .addValue("productId", result.productId)
                .addValue("totalScore", result.totalScore)
                .addValue("viewCount", result.viewCount)
                .addValue("likeCount", result.likeCount)
                .addValue("orderCount", result.orderCount)
                .addValue("rank", result.rank)
                .addValue("periodStartDate", periodStartDate)
                .addValue("periodEndDate", periodEndDate)
        }.toTypedArray()

        jdbcTemplate.batchUpdate(INSERT_SQL, params)
    }

    override fun deleteByPeriodStartDate(periodStartDate: LocalDate) {
        val params = MapSqlParameterSource("periodStartDate", periodStartDate)
        jdbcTemplate.update(DELETE_SQL, params)
    }

    companion object {
        private const val INSERT_SQL = """
            INSERT INTO mv_product_rank_weekly
                (product_id, total_score, view_count, like_count, order_count, `rank`, period_start_date, period_end_date, created_at)
            VALUES
                (:productId, :totalScore, :viewCount, :likeCount, :orderCount, :rank, :periodStartDate, :periodEndDate, NOW())
        """

        private const val DELETE_SQL = """
            DELETE FROM mv_product_rank_weekly WHERE period_start_date = :periodStartDate
        """
    }
}
