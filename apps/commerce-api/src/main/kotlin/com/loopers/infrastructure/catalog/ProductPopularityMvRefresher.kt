package com.loopers.infrastructure.catalog

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductPopularityMvRefresher(
    private val entityManager: EntityManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    fun refresh() {
        log.debug("product_popularity_mv 갱신 시작")

        entityManager.createNativeQuery("DELETE FROM product_popularity_mv").executeUpdate()

        entityManager.createNativeQuery(
            """
            INSERT INTO product_popularity_mv (product_id, brand_id, like_count, popularity_rank)
            SELECT p.id, p.brand_id, p.like_count,
                   ROW_NUMBER() OVER (ORDER BY p.like_count DESC, p.id DESC) AS popularity_rank
            FROM products p
            WHERE p.deleted_at IS NULL
            """.trimIndent(),
        ).executeUpdate()

        log.debug("product_popularity_mv 갱신 완료")
    }
}
