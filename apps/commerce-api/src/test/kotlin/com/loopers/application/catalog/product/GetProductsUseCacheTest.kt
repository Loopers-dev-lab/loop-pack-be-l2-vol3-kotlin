package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.data.redis.RedisConnectionFailureException
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetProductsUseCacheTest @Autowired constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val productRepository: ProductRepository,
    private val cacheManager: CacheManager,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
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

    private fun saveProduct(
        brandId: Long = 1L,
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
    ): Product {
        return productRepository.save(
            Product(refBrandId = BrandId(brandId), name = name, price = Money(price), stock = Stock(stock)),
        )
    }

    @Nested
    @DisplayName("@Cacheable — 상품 목록 캐시")
    inner class ProductListCache {

        @Test
        @DisplayName("동일한 조건으로 2회 조회하면 2번째는 캐시에서 반환된다")
        fun execute_secondCall_returnsCachedResult() {
            assumeTrue(isRedisAvailable(), "Redis를 사용할 수 없는 환경 — 테스트 건너뜀")

            // arrange
            val product = saveProduct(brandId = 99L, name = "캐시테스트상품", price = BigDecimal("129000"))

            // act — 1회차: DB 조회 후 캐시 저장
            val first = getProductsUseCase.execute(99L, "LATEST", 0, 10)
            assertThat(first.content).hasSize(1)
            assertThat(first.content[0].name).isEqualTo("캐시테스트상품")

            // DB 데이터를 변경하여 캐시와 DB 불일치 상태를 만든다
            val updated = Product(
                id = product.id,
                refBrandId = product.refBrandId,
                name = "DB변경후상품",
                price = product.price,
                stock = product.stock,
                status = product.status,
                likeCount = product.likeCount,
                deletedAt = product.deletedAt,
            )
            productRepository.save(updated)

            // act — 2회차: DB가 변경되었어도 캐시에서 이전 값이 반환되어야 함
            val second = getProductsUseCase.execute(99L, "LATEST", 0, 10)

            // assert — 캐시 적중 증명: DB 변경 후에도 캐시에 저장된 이전 이름이 반환된다
            assertThat(second.content).hasSize(1)
            assertThat(second.content[0].name).isEqualTo("캐시테스트상품")
        }

        @Test
        @DisplayName("brandId가 다르면 서로 다른 캐시 키를 사용한다")
        fun execute_differentBrandId_usesDifferentCacheKey() {
            assumeTrue(isRedisAvailable(), "Redis를 사용할 수 없는 환경 — 테스트 건너뜀")

            // arrange — 충돌 방지를 위해 높은 값의 brandId 사용
            saveProduct(brandId = 97L, name = "나이키 상품", price = BigDecimal("100000"))
            saveProduct(brandId = 96L, name = "아디다스 상품", price = BigDecimal("90000"))

            // act
            val brand97Result = getProductsUseCase.execute(97L, "LATEST", 0, 10)
            val brand96Result = getProductsUseCase.execute(96L, "LATEST", 0, 10)

            // assert — 각각 독립된 캐시 키로 분리되어 다른 결과 반환
            assertThat(brand97Result.content).hasSize(1)
            assertThat(brand97Result.content[0].name).isEqualTo("나이키 상품")
            assertThat(brand96Result.content).hasSize(1)
            assertThat(brand96Result.content[0].name).isEqualTo("아디다스 상품")
        }

        @Test
        @DisplayName("캐시를 무효화하면 다음 조회 시 최신 데이터를 반환한다")
        fun execute_afterCacheEvict_returnsUpdatedResult() {
            assumeTrue(isRedisAvailable(), "Redis를 사용할 수 없는 환경 — 테스트 건너뜀")

            // arrange — 격리된 brandId로 상품 저장 후 1회 조회하여 캐시 적재
            val product = saveProduct(brandId = 98L, name = "에어맥스 90", price = BigDecimal("129000"))
            val cached = getProductsUseCase.execute(98L, "LATEST", 0, 10)
            assertThat(cached.content).hasSize(1)
            assertThat(cached.content[0].name).isEqualTo("에어맥스 90")

            // DB에 수정된 상품 직접 저장
            val updated = Product(
                id = product.id,
                refBrandId = product.refBrandId,
                name = "에어맥스 95",
                price = product.price,
                stock = product.stock,
                status = product.status,
                likeCount = product.likeCount,
                deletedAt = product.deletedAt,
            )
            productRepository.save(updated)

            // 캐시 수동 무효화
            cacheManager.getCache("product:list")?.clear()

            // act — 캐시 evict 후 재조회
            val result = getProductsUseCase.execute(98L, "LATEST", 0, 10)

            // assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("에어맥스 95")
        }
    }
}
