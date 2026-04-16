package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.MonthlyRankingRepository
import com.loopers.domain.ranking.RankingEntry
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.YearMonth

@Repository
class MonthlyRankingJdbcRepository(
    private val jdbcTemplate: JdbcTemplate,
) : MonthlyRankingRepository {

    override fun findTopRankings(yearMonth: YearMonth, offset: Long, count: Long): List<RankingEntry> {
        val sql = """
            SELECT product_id, score
            FROM mv_product_rank_monthly
            WHERE `year_month` = ?
            ORDER BY rank_num
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            RankingEntry(
                productId = rs.getLong("product_id"),
                score = rs.getDouble("score"),
            )
        }, yearMonth.toString(), count, offset)
    }
}
