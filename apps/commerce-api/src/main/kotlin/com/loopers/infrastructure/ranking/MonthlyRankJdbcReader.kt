package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.MonthlyRankReader
import com.loopers.application.ranking.MonthlyRankView
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class MonthlyRankJdbcReader(
    private val jdbcTemplate: JdbcTemplate,
) : MonthlyRankReader {

    override fun findLatestRanks(offset: Long, limit: Int): List<MonthlyRankView> =
        jdbcTemplate.query(SELECT_LATEST_SQL, { rs, _ -> mapRow(rs) }, limit, offset)

    override fun countLatest(): Long =
        jdbcTemplate.queryForObject(COUNT_LATEST_SQL, Long::class.java) ?: 0L

    override fun findLatestRankOfProduct(productId: Long): Int? = try {
        jdbcTemplate.queryForObject(RANK_OF_PRODUCT_SQL, Int::class.java, productId)
    } catch (e: EmptyResultDataAccessException) {
        null
    }

    private fun mapRow(rs: ResultSet): MonthlyRankView = MonthlyRankView(
        productId = rs.getLong("product_id"),
        yearMonth = rs.getString("yearmonth"),
        viewCount = rs.getLong("view_count"),
        likeCount = rs.getLong("like_count"),
        orderCount = rs.getLong("order_count"),
        totalScore = rs.getDouble("total_score"),
        rankPosition = rs.getInt("rank_position"),
    )

    companion object {
        private const val SELECT_LATEST_SQL = """
            SELECT product_id, yearmonth,
                   view_count, like_count, order_count,
                   total_score, rank_position
            FROM mv_product_rank_monthly
            WHERE yearmonth = (SELECT MAX(yearmonth) FROM mv_product_rank_monthly)
            ORDER BY rank_position
            LIMIT ? OFFSET ?
        """

        private const val COUNT_LATEST_SQL = """
            SELECT COUNT(*) FROM mv_product_rank_monthly
            WHERE yearmonth = (SELECT MAX(yearmonth) FROM mv_product_rank_monthly)
        """

        private const val RANK_OF_PRODUCT_SQL = """
            SELECT rank_position FROM mv_product_rank_monthly
            WHERE yearmonth = (SELECT MAX(yearmonth) FROM mv_product_rank_monthly)
              AND product_id = ?
        """
    }
}
