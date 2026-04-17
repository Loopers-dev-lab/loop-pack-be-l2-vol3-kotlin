package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.WeeklyRankReader
import com.loopers.application.ranking.WeeklyRankView
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class WeeklyRankJdbcReader(
    private val jdbcTemplate: JdbcTemplate,
) : WeeklyRankReader {

    override fun findLatestRanks(offset: Long, limit: Int): List<WeeklyRankView> =
        jdbcTemplate.query(SELECT_LATEST_SQL, { rs, _ -> mapRow(rs) }, limit, offset)

    override fun countLatest(): Long =
        jdbcTemplate.queryForObject(COUNT_LATEST_SQL, Long::class.java) ?: 0L

    override fun findLatestRankOfProduct(productId: Long): Int? = try {
        jdbcTemplate.queryForObject(RANK_OF_PRODUCT_SQL, Int::class.java, productId)
    } catch (e: EmptyResultDataAccessException) {
        null
    }

    private fun mapRow(rs: ResultSet): WeeklyRankView = WeeklyRankView(
        productId = rs.getLong("product_id"),
        weekStart = rs.getDate("week_start").toLocalDate(),
        weekEnd = rs.getDate("week_end").toLocalDate(),
        viewCount = rs.getLong("view_count"),
        likeCount = rs.getLong("like_count"),
        orderCount = rs.getLong("order_count"),
        totalScore = rs.getDouble("total_score"),
        rankPosition = rs.getInt("rank_position"),
    )

    companion object {
        private const val SELECT_LATEST_SQL = """
            SELECT product_id, week_start, week_end,
                   view_count, like_count, order_count,
                   total_score, rank_position
            FROM mv_product_rank_weekly
            WHERE week_end = (SELECT MAX(week_end) FROM mv_product_rank_weekly)
            ORDER BY rank_position
            LIMIT ? OFFSET ?
        """

        private const val COUNT_LATEST_SQL = """
            SELECT COUNT(*) FROM mv_product_rank_weekly
            WHERE week_end = (SELECT MAX(week_end) FROM mv_product_rank_weekly)
        """

        private const val RANK_OF_PRODUCT_SQL = """
            SELECT rank_position FROM mv_product_rank_weekly
            WHERE week_end = (SELECT MAX(week_end) FROM mv_product_rank_weekly)
              AND product_id = ?
        """
    }
}
