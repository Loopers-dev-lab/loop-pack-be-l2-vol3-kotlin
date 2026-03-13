package com.loopers.infrastructure.catalog

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration

@Component
class ProductPopularityMvRefresher(
    private val entityManager: EntityManager,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun refresh() {
        val acquired = redisTemplate.opsForValue()
            .setIfAbsent(LOCK_KEY, "locked", Duration.ofSeconds(LOCK_TTL_SECONDS)) ?: false

        if (!acquired) {
            log.debug("product_popularity_mv 갱신 스킵 (다른 인스턴스에서 실행 중)")
            return
        }

        try {
            log.debug("product_popularity_mv 갱신 시작")

            val sourceCount = (entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM products WHERE deleted_at IS NULL",
            ).singleResult as Number).toLong()

            entityManager.createNativeQuery("DELETE FROM product_popularity_mv").executeUpdate()

            val insertedCount = entityManager.createNativeQuery(
                """
                INSERT INTO product_popularity_mv (product_id, brand_id, like_count, popularity_rank)
                SELECT p.id, p.brand_id, COALESCE(lc.cnt, 0) AS like_count,
                       ROW_NUMBER() OVER (ORDER BY COALESCE(lc.cnt, 0) DESC, p.id DESC) AS popularity_rank
                FROM products p
                LEFT JOIN (
                    SELECT product_id, COUNT(*) AS cnt
                    FROM product_likes
                    WHERE deleted_at IS NULL
                    GROUP BY product_id
                ) lc ON p.id = lc.product_id
                WHERE p.deleted_at IS NULL
                """.trimIndent(),
            ).executeUpdate()

            if (sourceCount > 0 && insertedCount == 0) {
                throw IllegalStateException(
                    "product_popularity_mv 갱신 실패: 소스 ${sourceCount}건 존재하나 삽입 0건",
                )
            }

            log.debug("product_popularity_mv 갱신 완료 ({}건)", insertedCount)
        } finally {
            redisTemplate.delete(LOCK_KEY)
        }
    }

    companion object {
        private const val LOCK_KEY = "lock:product_popularity_mv_refresh"
        private const val LOCK_TTL_SECONDS = 30L
    }
}
