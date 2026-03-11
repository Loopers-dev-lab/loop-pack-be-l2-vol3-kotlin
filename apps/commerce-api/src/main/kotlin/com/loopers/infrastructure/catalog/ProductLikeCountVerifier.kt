package com.loopers.infrastructure.catalog

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductLikeCountVerifier(
    private val entityManager: EntityManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun verify(): List<MismatchResult> {
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createNativeQuery(
            """
            SELECT p.id AS product_id,
                   p.like_count AS denormalized_count,
                   COALESCE(actual.cnt, 0) AS actual_count
            FROM products p
            LEFT JOIN (
                SELECT product_id, COUNT(*) AS cnt
                FROM product_likes
                WHERE deleted_at IS NULL
                GROUP BY product_id
            ) actual ON p.id = actual.product_id
            WHERE p.deleted_at IS NULL
              AND p.like_count != COALESCE(actual.cnt, 0)
            ORDER BY ABS(p.like_count - COALESCE(actual.cnt, 0)) DESC
            LIMIT 100
            """.trimIndent(),
        ).resultList as List<Array<Any>>

        val mismatches = rows.map { row ->
            MismatchResult(
                productId = (row[0] as Number).toLong(),
                denormalizedCount = (row[1] as Number).toInt(),
                actualCount = (row[2] as Number).toInt(),
            )
        }

        if (mismatches.isNotEmpty()) {
            log.warn("like_count 불일치 발견: {}건", mismatches.size)
            mismatches.take(5).forEach {
                log.warn("  productId={}, denormalized={}, actual={}", it.productId, it.denormalizedCount, it.actualCount)
            }
        }

        return mismatches
    }

    data class MismatchResult(
        val productId: Long,
        val denormalizedCount: Int,
        val actualCount: Int,
    )
}
