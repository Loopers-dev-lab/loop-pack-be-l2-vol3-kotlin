package com.loopers.infrastructure.ranking

import com.loopers.config.ranking.RankingReadProperties
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingReadModel
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.domain.ranking.RankedProductsWithCount
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.pow

@Repository
class ProductRankingRepositoryImpl(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val rankingReadProperties: RankingReadProperties,
) : ProductRankingRepository {

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private const val TEMP_KEY_PREFIX = "ranking:tmp:"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }

    override fun getRankedProducts(
        processingDate: LocalDate,
        page: Int,
        size: Int,
    ): List<ProductRankingReadModel> {
        if (!rankingReadProperties.isSlidingWindow()) {
            return dailyRankedProducts(processingDate, page, size)
        }

        val tempKey = buildSlidingWindowKey(processingDate) ?: return emptyList()
        try {
            return readRankedProducts(tempKey, page, size)
        } finally {
            redisTemplate.delete(tempKey)
        }
    }

    override fun getRank(processingDate: LocalDate, productId: Long): Long? {
        if (!rankingReadProperties.isSlidingWindow()) {
            return dailyRank(processingDate, productId)
        }

        val tempKey = buildSlidingWindowKey(processingDate) ?: return null
        try {
            return dailyRank(tempKey, productId)
        } finally {
            redisTemplate.delete(tempKey)
        }
    }

    override fun count(processingDate: LocalDate): Long {
        if (!rankingReadProperties.isSlidingWindow()) {
            return redisTemplate.opsForZSet().zCard(key(processingDate)) ?: 0L
        }

        val tempKey = buildSlidingWindowKey(processingDate) ?: return 0L
        try {
            return redisTemplate.opsForZSet().zCard(tempKey) ?: 0L
        } finally {
            redisTemplate.delete(tempKey)
        }
    }

    override fun getRankedProductsWithCount(processingDate: LocalDate, page: Int, size: Int): RankedProductsWithCount {
        if (!rankingReadProperties.isSlidingWindow()) {
            val products = dailyRankedProducts(processingDate, page, size)
            val count = redisTemplate.opsForZSet().zCard(key(processingDate)) ?: 0L
            return RankedProductsWithCount(products, count)
        }

        val tempKey = buildSlidingWindowKey(processingDate) ?: return RankedProductsWithCount(emptyList(), 0L)
        try {
            val products = readRankedProducts(tempKey, page, size)
            val count = redisTemplate.opsForZSet().zCard(tempKey) ?: 0L
            return RankedProductsWithCount(products, count)
        } finally {
            redisTemplate.delete(tempKey)
        }
    }

    // ── Daily ─────────────────────────────────────────────────

    private fun dailyRankedProducts(
        processingDate: LocalDate,
        page: Int,
        size: Int,
    ): List<ProductRankingReadModel> = readRankedProducts(key(processingDate), page, size)

    private fun readRankedProducts(redisKey: String, page: Int, size: Int): List<ProductRankingReadModel> {
        val start = page.toLong() * size
        val end = start + size - 1

        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(redisKey, start, end)
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

    private fun dailyRank(processingDate: LocalDate, productId: Long): Long? {
        return redisTemplate.opsForZSet()
            .reverseRank(key(processingDate), productId.toString())
            ?.plus(1)
    }

    private fun dailyRank(redisKey: String, productId: Long): Long? {
        return redisTemplate.opsForZSet()
            .reverseRank(redisKey, productId.toString())
            ?.plus(1)
    }

    // ── Sliding Window ────────────────────────────────────────

    private fun buildSlidingWindowKey(anchorDate: LocalDate): String? {
        val windowDays = rankingReadProperties.slidingWindow.windowDays
        val decayFactor = rankingReadProperties.slidingWindow.decayFactor

        val sourceKeys = (0..windowDays).map { daysAgo ->
            key(anchorDate.minusDays(daysAgo.toLong()))
        }
        val weights = (0..windowDays).map { daysAgo ->
            decayFactor.pow(daysAgo)
        }.toDoubleArray()

        val tempKey = "$TEMP_KEY_PREFIX${UUID.randomUUID()}"
        val count = zunionstore(tempKey, sourceKeys, weights)
        if (count == 0L) {
            redisTemplate.delete(tempKey)
            return null
        }
        // Set TTL to prevent leak in case of app crash between zunionstore and delete
        redisTemplate.expire(tempKey, Duration.ofSeconds(60))
        return tempKey
    }

    private fun zunionstore(destKey: String, sourceKeys: List<String>, weights: DoubleArray): Long {
        val result = redisTemplate.execute(
            object : RedisCallback<Long> {
                override fun doInRedis(connection: RedisConnection): Long {
                    return connection.zSetCommands().zUnionStore(
                        destKey.toByteArray(Charsets.UTF_8),
                        Aggregate.SUM,
                        Weights.of(*weights),
                        *sourceKeys.map { it.toByteArray(Charsets.UTF_8) }.toTypedArray(),
                    ) ?: 0L
                }
            },
        )
        return result ?: 0L
    }

    private fun key(processingDate: LocalDate): String = "$KEY_PREFIX${processingDate.format(DATE_FORMATTER)}"
}
