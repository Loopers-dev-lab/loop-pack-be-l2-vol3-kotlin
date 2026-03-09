package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.utils.DatabaseCleanUp
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import kotlin.math.pow
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductSeedingAndExplainTest @Autowired constructor(
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val random = Random(42)

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    /**
     * 멱법칙(power law) 분포로 좋아요 수를 생성한다.
     * 소수의 상품에 좋아요가 집중되는 실제 데이터 분포를 모사한다.
     */
    private fun powerLawLikeCount(): Int {
        val u = random.nextDouble()
        val maxLike = 10_000.0
        return (maxLike * (1 - u).pow(3.0)).toInt().coerceIn(0, 10_000)
    }

    private fun randomStatus(): Product.ProductStatus {
        val roll = random.nextInt(100)
        return when {
            roll < 90 -> Product.ProductStatus.ON_SALE
            roll < 98 -> Product.ProductStatus.SOLD_OUT
            else -> Product.ProductStatus.HIDDEN
        }
    }

    @BeforeEach
    fun seedData() {
        // 브랜드 50~100개, 브랜드별 상품 수 편차 (10 ~ 5000개)
        val brandCount = random.nextInt(50, 101)
        val brandIds = (1L..brandCount.toLong()).toList()

        // 브랜드별 상품 비율(가중치)을 미리 정해서 편차를 만든다
        val brandWeights = brandIds.map { random.nextInt(10, 5001) }
        val totalWeight = brandWeights.sum().toDouble()

        val allEntities = mutableListOf<ProductEntity>()
        val totalCount = 100_000
        var remaining = totalCount

        brandIds.forEachIndexed { index, brandId ->
            val count = if (index == brandIds.lastIndex) {
                remaining
            } else {
                (totalCount * brandWeights[index] / totalWeight).toInt().coerceAtLeast(1)
            }
            remaining -= count

            repeat(count) {
                val price = BigDecimal(random.nextLong(1_000L, 1_000_001L))
                allEntities.add(
                    ProductEntity(
                        refBrandId = brandId,
                        name = "상품-$brandId-$it",
                        price = price,
                        stock = random.nextInt(0, 1000),
                        status = randomStatus(),
                        likeCount = powerLawLikeCount(),
                    ),
                )
            }
        }

        // 1000개씩 배치 저장
        allEntities.chunked(1_000).forEach { chunk ->
            productJpaRepository.saveAll(chunk)
        }
        productJpaRepository.flush()
    }

    @Nested
    @DisplayName("EXPLAIN 분석")
    inner class ExplainAnalysis {

        @Test
        @Transactional
        @DisplayName("브랜드 필터 + 좋아요 정렬 쿼리는 풀 스캔(ALL)이 아닌 인덱스를 사용한다")
        fun explainBrandFilterOrderByLikes() {
            // arrange
            val brandId = 1L

            // act
            val sql = """
                EXPLAIN SELECT * FROM products
                WHERE deleted_at IS NULL
                  AND status != 'HIDDEN'
                  AND ref_brand_id = $brandId
                ORDER BY like_count DESC
                LIMIT 20
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val result = entityManager.createNativeQuery(sql).resultList as List<Array<Any?>>

            // assert
            log.info("[EXPLAIN] 브랜드 필터 + 좋아요 정렬:")
            result.forEach { row -> log.info(row.joinToString(" | ")) }

            val types = result.map { row -> row[3]?.toString() ?: "" }
            assertThat(types).noneMatch { it == "ALL" }
        }

        @Test
        @Transactional
        @DisplayName("브랜드 필터 + 가격 오름차순 쿼리는 풀 스캔(ALL)이 아닌 인덱스를 사용한다")
        fun explainBrandFilterOrderByPrice() {
            // arrange
            val brandId = 1L

            // act
            val sql = """
                EXPLAIN SELECT * FROM products
                WHERE deleted_at IS NULL
                  AND status != 'HIDDEN'
                  AND ref_brand_id = $brandId
                ORDER BY price ASC
                LIMIT 20
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val result = entityManager.createNativeQuery(sql).resultList as List<Array<Any?>>

            // assert
            log.info("[EXPLAIN] 브랜드 필터 + 가격 정렬:")
            result.forEach { row -> log.info(row.joinToString(" | ")) }

            val types = result.map { row -> row[3]?.toString() ?: "" }
            assertThat(types).noneMatch { it == "ALL" }
        }

        @Test
        @Transactional
        @DisplayName("최신순 전체 조회 쿼리는 풀 스캔(ALL)이 아닌 인덱스를 사용한다")
        fun explainAllOrderByCreatedAt() {
            // act
            val sql = """
                EXPLAIN SELECT * FROM products
                WHERE deleted_at IS NULL
                  AND status != 'HIDDEN'
                ORDER BY created_at DESC
                LIMIT 20
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val result = entityManager.createNativeQuery(sql).resultList as List<Array<Any?>>

            // assert
            log.info("[EXPLAIN] 최신순 전체 조회:")
            result.forEach { row -> log.info(row.joinToString(" | ")) }

            val types = result.map { row -> row[3]?.toString() ?: "" }
            assertThat(types).noneMatch { it == "ALL" }
        }

        @Test
        @Transactional
        @DisplayName("좋아요 내림차순 깊은 페이지 쿼리는 풀 스캔(ALL)이 아닌 인덱스를 사용한다")
        fun explainDeepPageOrderByLikes() {
            // act
            val sql = """
                EXPLAIN SELECT * FROM products
                WHERE deleted_at IS NULL
                  AND status != 'HIDDEN'
                ORDER BY like_count DESC
                LIMIT 20 OFFSET 10000
            """.trimIndent()

            @Suppress("UNCHECKED_CAST")
            val result = entityManager.createNativeQuery(sql).resultList as List<Array<Any?>>

            // assert
            log.info("[EXPLAIN] 좋아요 내림차순 깊은 페이지:")
            result.forEach { row -> log.info(row.joinToString(" | ")) }

            val types = result.map { row -> row[3]?.toString() ?: "" }
            assertThat(types).noneMatch { it == "ALL" }
        }
    }
}
