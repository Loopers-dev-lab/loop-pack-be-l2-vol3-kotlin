package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.WeeklyRankingRepository
import com.loopers.domain.ranking.YearWeek
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class WeeklyRankingJdbcRepository(
    private val jdbcTemplate: JdbcTemplate,
) : WeeklyRankingRepository {

    override fun findTopRankings(yearWeek: YearWeek, offset: Long, count: Long): List<RankingEntry> {
        val sql = """
            SELECT product_id, score
            FROM mv_product_rank_weekly
            WHERE year_week = ?
            ORDER BY rank_num
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            RankingEntry(
                productId = rs.getLong("product_id"),
                score = rs.getDouble("score"),
            )
        }, yearWeek.toString(), count, offset)
    }
}
