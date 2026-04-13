package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.domain.ranking.RankedProduct
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class RedisProductRankingQueryRepository(
    private val redisTemplate: RedisTemplate<String, String>,
) : ProductRankingQueryRepository {
    override fun getTopRanked(
        date: LocalDate,
        offset: Long,
        count: Long,
    ): List<RankedProduct> {
        val key = buildKey(date)
        val tuples = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, offset, offset + count - 1)
            ?: return emptyList()

        return tuples.mapIndexed { index, tuple ->
            RankedProduct(
                productId = tuple.value!!.toLong(),
                score = tuple.score!!,
                rank = offset + index + 1,
            )
        }
    }

    override fun getRank(
        date: LocalDate,
        productId: Long,
    ): Long? {
        val key = buildKey(date)
        val zeroBasedRank = redisTemplate.opsForZSet()
            .reverseRank(key, productId.toString())
        return zeroBasedRank?.plus(1)
    }

    override fun getTotalCount(date: LocalDate): Long {
        val key = buildKey(date)
        return redisTemplate.opsForZSet().size(key) ?: 0
    }

    companion object {
        private const val KEY_PREFIX = "ranking:all:"

        fun buildKey(date: LocalDate): String =
            "$KEY_PREFIX${date.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }
}
