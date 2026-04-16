package com.loopers.support

import com.loopers.batch.job.ranking.RankingScorePolicy
import com.loopers.infrastructure.persistence.metrics.ProductMetricsEntity
import com.loopers.infrastructure.persistence.metrics.ProductMetricsJpaRepository
import org.springframework.stereotype.Component
import kotlin.random.Random

/**
 * 테스트/벤치마크용 product_metrics seed 헬퍼.
 *
 * 결정적 분포 (`seed` 고정) 로 N 개 row 를 적재해 동일한 입력에 대한 출력 검증을 가능하게 한다.
 */
@Component
class ProductMetricsSeeder(
    private val repository: ProductMetricsJpaRepository,
) {

    /**
     * @param count 생성할 product 수
     * @param seed  Random seed (고정 시 재현 가능)
     * @return seed 된 product 의 (productId, expectedScore) — 검증용
     */
    fun seedRandom(count: Int, seed: Long = 42L): List<Pair<Long, Double>> {
        repository.deleteAllInBatch()
        val rng = Random(seed)
        val entities = (1..count).map { idx ->
            val productId = idx.toLong()
            val viewCount = rng.nextLong(0, 10_000)
            val likeCount = rng.nextLong(0, 1_000)
            val salesCount = rng.nextLong(0, 100)
            val salesAmount = salesCount * rng.nextLong(1_000, 50_000)
            ProductMetricsEntity(
                productId = productId,
                viewCount = viewCount,
                likeCount = likeCount,
                salesCount = salesCount,
                salesAmount = salesAmount,
            )
        }
        repository.saveAll(entities)

        return entities.map { it.productId to RankingScorePolicy.score(it.viewCount, it.likeCount, it.salesCount) }
    }
}
