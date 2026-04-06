package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingReadModel
import com.loopers.domain.ranking.ProductRankingRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class ProductRankingRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) : ProductRankingRepository {

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }

    override fun getRankedProducts(processingDate: LocalDate, page: Int, size: Int): List<ProductRankingReadModel> {
        val start = page.toLong() * size
        val end = start + size - 1

        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(key(processingDate), start, end)
            ?.mapIndexedNotNull { index, tuple ->
                val productId = tuple.value?.toLongOrNull() ?: return@mapIndexedNotNull null
                ProductRankingReadModel(
                    productId = productId,
                    rank = start + index + 1,
                    score = tuple.score ?: 0.0,
                )
            }
            ?: emptyList()
    }

    override fun getRank(processingDate: LocalDate, productId: Long): Long? {
        return redisTemplate.opsForZSet()
            .reverseRank(key(processingDate), productId.toString())
            ?.plus(1)
    }

    override fun count(processingDate: LocalDate): Long {
        return redisTemplate.opsForZSet().zCard(key(processingDate)) ?: 0L
    }

    private fun key(processingDate: LocalDate): String = "$KEY_PREFIX${processingDate.format(DATE_FORMATTER)}"
}
