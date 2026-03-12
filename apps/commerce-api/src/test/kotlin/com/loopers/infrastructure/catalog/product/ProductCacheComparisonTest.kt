package com.loopers.infrastructure.catalog.product

import com.loopers.application.catalog.product.GetProductUseCase
import com.loopers.application.catalog.product.GetProductsUseCase
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.common.vo.ProductId
import com.loopers.infrastructure.catalog.brand.BrandEntity
import com.loopers.infrastructure.catalog.brand.BrandJpaRepository
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.data.redis.RedisConnectionFailureException
import java.math.BigDecimal
import kotlin.system.measureTimeMillis

@Tag("benchmark")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductCacheComparisonTest @Autowired constructor(
    private val getProductUseCase: GetProductUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val productCacheRepository: ProductCacheRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val cacheManager: CacheManager,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun isRedisAvailable(): Boolean {
        return try {
            redisCleanUp.truncateAll()
            true
        } catch (e: RedisConnectionFailureException) {
            false
        }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        try {
            redisCleanUp.truncateAll()
        } catch (e: RedisConnectionFailureException) {
            // Redis 미사용 환경에서는 무시
        }
    }

    @Nested
    @DisplayName("캐시 성능 비교")
    inner class CachePerformanceComparison {

        @Test
        @DisplayName("상품 상세 조회 - 캐시 미스 vs 캐시 히트 응답 시간 비교")
        fun productDetailCacheMissVsHit() {
            assumeTrue(isRedisAvailable(), "Redis를 사용할 수 없는 환경 — 테스트 건너뜀")

            // arrange: 브랜드 1개 + 상품 1개 DB에 저장
            val brandEntity = brandJpaRepository.save(BrandEntity(name = "나이키"))
            val productEntity = productJpaRepository.save(
                ProductEntity(
                    refBrandId = brandEntity.id,
                    name = "에어맥스 90",
                    price = BigDecimal("129000"),
                    stock = 100,
                    status = Product.ProductStatus.ON_SALE,
                ),
            )
            val productId = productEntity.id

            // 워밍업: JIT 컴파일 영향을 줄이기 위해 1회 선행 호출
            getProductUseCase.execute(productId)
            productCacheRepository.evictProductDetail(ProductId(productId))

            val repeatCount = 10

            // act 1: 캐시 미스 측정 (매 호출 전 캐시 evict)
            val missTimes = LongArray(repeatCount)
            repeat(repeatCount) { i ->
                productCacheRepository.evictProductDetail(ProductId(productId))
                missTimes[i] = measureTimeMillis {
                    getProductUseCase.execute(productId)
                }
            }

            // act 2: 캐시 히트 측정 (캐시가 채워진 상태에서 반복 호출)
            // 첫 호출로 캐시 적재
            getProductUseCase.execute(productId)
            val hitTimes = LongArray(repeatCount)
            repeat(repeatCount) { i ->
                hitTimes[i] = measureTimeMillis {
                    getProductUseCase.execute(productId)
                }
            }

            val missAvg = missTimes.average()
            val hitAvg = hitTimes.average()
            val improvementPercent = if (missAvg > 0) ((missAvg - hitAvg) / missAvg * 100).toLong() else 0L

            // assert + log
            println("=== 상품 상세 조회 캐시 성능 비교 ===")
            println("캐시 미스 개별(ms): ${missTimes.joinToString(", ")}")
            println("캐시 히트 개별(ms): ${hitTimes.joinToString(", ")}")
            println("캐시 미스 평균: ${String.format("%.2f", missAvg)}ms (측정 ${repeatCount}회)")
            println("캐시 히트 평균: ${String.format("%.2f", hitAvg)}ms (측정 ${repeatCount}회)")
            println("개선율: $improvementPercent%")

            assertThat(hitAvg).isLessThan(missAvg)
        }

        @Test
        @DisplayName("상품 목록 조회 - @Cacheable 캐시 미스 vs 캐시 히트 응답 시간 비교")
        fun productListCacheMissVsHit() {
            assumeTrue(isRedisAvailable(), "Redis를 사용할 수 없는 환경 — 테스트 건너뜀")

            // arrange: 브랜드 1개 + 상품 1000개 DB에 저장
            val brandEntity = brandJpaRepository.save(BrandEntity(name = "아디다스"))
            val brandId = brandEntity.id

            val entities = (1..1_000).map { i ->
                ProductEntity(
                    refBrandId = brandId,
                    name = "상품-$i",
                    price = BigDecimal((10_000 + i * 100).toLong()),
                    stock = 50,
                    status = Product.ProductStatus.ON_SALE,
                )
            }
            entities.chunked(200).forEach { chunk ->
                productJpaRepository.saveAll(chunk)
            }
            productJpaRepository.flush()

            val productListCache = cacheManager.getCache("product:list")

            // 워밍업: JIT 컴파일 영향을 줄이기 위해 1회 선행 호출
            getProductsUseCase.execute(brandId, "LATEST", 0, 20)
            productListCache?.clear()

            val repeatCount = 10

            // act 1: 캐시 미스 측정 (매 호출 전 캐시 clear)
            val missTimes = LongArray(repeatCount)
            repeat(repeatCount) { i ->
                productListCache?.clear()
                missTimes[i] = measureTimeMillis {
                    getProductsUseCase.execute(brandId, "LATEST", 0, 20)
                }
            }

            // act 2: 캐시 히트 측정 (캐시가 채워진 상태에서 반복 호출)
            // 첫 호출로 캐시 적재
            getProductsUseCase.execute(brandId, "LATEST", 0, 20)
            val hitTimes = LongArray(repeatCount)
            repeat(repeatCount) { i ->
                hitTimes[i] = measureTimeMillis {
                    getProductsUseCase.execute(brandId, "LATEST", 0, 20)
                }
            }

            val missAvg = missTimes.average()
            val hitAvg = hitTimes.average()
            val improvementPercent = if (missAvg > 0) ((missAvg - hitAvg) / missAvg * 100).toLong() else 0L

            // assert + log
            println("=== 상품 목록 조회 캐시 성능 비교 ===")
            println("캐시 미스 개별(ms): ${missTimes.joinToString(", ")}")
            println("캐시 히트 개별(ms): ${hitTimes.joinToString(", ")}")
            println("캐시 미스 평균: ${String.format("%.2f", missAvg)}ms (측정 ${repeatCount}회)")
            println("캐시 히트 평균: ${String.format("%.2f", hitAvg)}ms (측정 ${repeatCount}회)")
            println("개선율: $improvementPercent%")

            assertThat(hitAvg).isLessThan(missAvg)
        }
    }
}
