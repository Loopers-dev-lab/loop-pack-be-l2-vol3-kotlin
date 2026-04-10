package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.ranking.RankingEntry
import com.loopers.domain.ranking.RankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RankingRedisRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    @Qualifier(REDIS_TEMPLATE_MASTER)
    private val masterRedisTemplate: RedisTemplate<String, String>,
    private val jdbcTemplate: JdbcTemplate,
) : RankingRepository {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private const val TTL_SECONDS = 2 * 24 * 60 * 60L
    }

    override fun getTopRankings(date: LocalDate, offset: Long, count: Long): List<RankingEntry> {
        val key = RedisKeys.rankingKey(date.format(DATE_FORMATTER))
        val result = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, offset, offset + count - 1) ?: emptySet()
        return result.mapNotNull { typedTuple ->
            val productId = typedTuple.value?.toLongOrNull() ?: return@mapNotNull null
            val score = typedTuple.score ?: return@mapNotNull null
            RankingEntry(productId, score)
        }
    }

    override fun getRank(date: LocalDate, productId: Long): Long? {
        val key = RedisKeys.rankingKey(date.format(DATE_FORMATTER))
        return redisTemplate.opsForZSet().reverseRank(key, productId.toString())
    }

    private val carryOverScript = RedisScript.of<Long>(
        ClassPathResource("scripts/ranking_carry_over.lua"),
        Long::class.java,
    )

    override fun carryOver(fromDate: LocalDate, toDate: LocalDate, weight: Double) {
        val fromKey = RedisKeys.rankingKey(fromDate.format(DATE_FORMATTER))
        val toKey = RedisKeys.rankingKey(toDate.format(DATE_FORMATTER))
        masterRedisTemplate.execute(
            carryOverScript,
            listOf(fromKey, toKey),
            weight.toString(),
            TTL_SECONDS.toString(),
        )
    }

    override fun getTopRankingsFromDb(offset: Long, count: Long): List<RankingEntry> {
        val sql = """
            SELECT pm.product_id,
                   (pm.view_count * 0.1 + pm.like_count * 0.2 + pm.sales_count * 0.7) AS score
            FROM product_metrics pm
            INNER JOIN products p ON pm.product_id = p.id
            WHERE p.deleted_at IS NULL
            ORDER BY score DESC
            LIMIT ? OFFSET ?
        """.trimIndent()
        return jdbcTemplate.query(sql, { rs, _ ->
            RankingEntry(
                productId = rs.getLong("product_id"),
                score = rs.getDouble("score"),
            )
        }, count, offset)
    }
}
