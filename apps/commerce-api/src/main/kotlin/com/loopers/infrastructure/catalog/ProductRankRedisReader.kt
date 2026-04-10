package com.loopers.infrastructure.catalog

import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 상품 랭킹 ZSET 조회 전용 Reader.
 *
 * 적재(write)는 commerce-streamer의 ProductRankRedisRepository에서 담당하고,
 * 여기서는 조회(read)만 수행한다. 키 포맷은 적재 측과 동일해야 한다.
 *
 * - rank:all:{yyyyMMdd} — 가중치 합산 score 기반 랭킹 (조회 대상)
 */
@Component
class ProductRankRedisReader(
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val KEY_PREFIX = "rank"
        private const val TYPE_ALL = "all"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    }

    /**
     * 지정 날짜의 랭킹을 페이지 단위로 조회한다.
     *
     * @param page 0-based 페이지 인덱스
     * @return (productId, score, 0-based 절대 순위) 리스트. 데이터 없으면 빈 리스트
     */
    fun getRankingPage(date: LocalDate, page: Int, size: Int): List<RankedProductEntry> {
        if (page < 0 || size <= 0) return emptyList()

        val key = key(date)
        val start = (page.toLong() * size)
        val end = start + size - 1

        val tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end) ?: return emptyList()

        return tuples.mapIndexedNotNull { index, tuple ->
            val productId = tuple.value?.toLongOrNull() ?: return@mapIndexedNotNull null
            val score = tuple.score ?: return@mapIndexedNotNull null
            RankedProductEntry(
                productId = productId,
                score = score,
                rank = start + index,
            )
        }
    }

    /**
     * 지정 날짜의 랭킹에 포함된 상품 전체 개수.
     */
    fun getTotalCount(date: LocalDate): Long {
        return redisTemplate.opsForZSet().zCard(key(date)) ?: 0L
    }

    /**
     * 지정 날짜에서 특정 상품의 0-based 순위를 조회한다.
     * 랭킹에 없으면 null.
     */
    fun getRank(productId: Long, date: LocalDate): Long? {
        return redisTemplate.opsForZSet().reverseRank(key(date), productId.toString())
    }

    private fun key(date: LocalDate): String =
        "$KEY_PREFIX:$TYPE_ALL:${date.format(DATE_FORMAT)}"
}

data class RankedProductEntry(
    val productId: Long,
    val score: Double,
    val rank: Long,
)
