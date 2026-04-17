package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankAggregation
import org.springframework.batch.item.ItemReader
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate

class RankingAggregationReader(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val startDate: LocalDate,
    private val endDate: LocalDate,
) : ItemReader<ProductRankAggregation> {

    private var results: Iterator<ProductRankAggregation>? = null

    override fun read(): ProductRankAggregation? {
        if (results == null) {
            results = executeAggregationQuery().iterator()
        }
        return if (results!!.hasNext()) results!!.next() else null
    }

    private fun executeAggregationQuery(): List<ProductRankAggregation> {
        val weights = loadWeights()
        val params = MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate)
            .addValue("viewWeight", weights.viewWeight)
            .addValue("likeWeight", weights.likeWeight)
            .addValue("orderWeight", weights.orderWeight)

        return jdbcTemplate.query(AGGREGATION_SQL, params) { rs, _ ->
            ProductRankAggregation(
                productId = rs.getLong("product_id"),
                totalScore = rs.getDouble("total_score"),
                viewCount = rs.getInt("view_count"),
                likeCount = rs.getInt("like_count"),
                orderCount = rs.getInt("order_count"),
            )
        }
    }

    private fun loadWeights(): ScoreWeights {
        val configMap = jdbcTemplate.query(WEIGHT_SQL, MapSqlParameterSource()) { rs, _ ->
            rs.getString("config_key") to rs.getDouble("config_value")
        }.toMap()

        return ScoreWeights(
            viewWeight = configMap[VIEW_WEIGHT_KEY] ?: DEFAULT_VIEW_WEIGHT,
            likeWeight = configMap[LIKE_WEIGHT_KEY] ?: DEFAULT_LIKE_WEIGHT,
            orderWeight = configMap[ORDER_WEIGHT_KEY] ?: DEFAULT_ORDER_WEIGHT,
        )
    }

    private data class ScoreWeights(
        val viewWeight: Double,
        val likeWeight: Double,
        val orderWeight: Double,
    )

    companion object {
        private const val DEFAULT_VIEW_WEIGHT = 0.1
        private const val DEFAULT_LIKE_WEIGHT = 0.2
        private const val DEFAULT_ORDER_WEIGHT = 0.6

        private const val VIEW_WEIGHT_KEY = "VIEW_WEIGHT"
        private const val LIKE_WEIGHT_KEY = "LIKE_WEIGHT"
        private const val ORDER_WEIGHT_KEY = "ORDER_WEIGHT"

        private const val WEIGHT_SQL = """
            SELECT config_key, config_value FROM ranking_score_config
        """

        private const val AGGREGATION_SQL = """
            SELECT
                rel.product_id,
                SUM(CASE WHEN rel.event_type = 'VIEW'  THEN rel.event_value * :viewWeight ELSE 0 END) +
                SUM(CASE WHEN rel.event_type = 'LIKE'  THEN rel.event_value * :likeWeight ELSE 0 END) +
                SUM(CASE WHEN rel.event_type = 'ORDER' THEN rel.event_value * :orderWeight ELSE 0 END) AS total_score,
                COUNT(CASE WHEN rel.event_type = 'VIEW'  THEN 1 END) AS view_count,
                COUNT(CASE WHEN rel.event_type = 'LIKE'  THEN 1 END) AS like_count,
                COUNT(CASE WHEN rel.event_type = 'ORDER' THEN 1 END) AS order_count
            FROM ranking_event_log rel
            WHERE rel.occurred_date BETWEEN :startDate AND :endDate
            GROUP BY rel.product_id
            ORDER BY total_score DESC
            LIMIT 100
        """
    }
}
