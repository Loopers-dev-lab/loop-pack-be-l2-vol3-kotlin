package com.loopers.infrastructure.product

import com.loopers.application.product.ProductInfo
import com.loopers.support.cache.CachedPage
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest
class ProductCacheRepositoryTest @Autowired constructor(
    private val productCacheRepository: ProductCacheRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun createProductInfo(
        id: Long = 1L,
        brandId: Long = 1L,
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
        likeCount: Int = 0,
    ): ProductInfo {
        val now = ZonedDateTime.now()
        return ProductInfo(
            id = id,
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            likeCount = likeCount,
            description = "나이키 에어맥스 90",
            imageUrl = "https://example.com/airmax90.jpg",
            createdAt = now,
            updatedAt = now,
        )
    }

    @DisplayName("상품 상세 캐시")
    @Nested
    inner class ProductDetailCache {

        @DisplayName("저장 후 조회하면, 동일한 데이터가 반환된다.")
        @Test
        fun returnsData_whenCacheHit() {
            // arrange
            val productId = 1L
            val info = createProductInfo(id = productId)

            // act
            productCacheRepository.setProductDetail(productId, info)
            val result = productCacheRepository.getProductDetail(productId)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result!!.id).isEqualTo(productId) },
                { assertThat(result!!.name).isEqualTo("에어맥스 90") },
                { assertThat(result!!.price).isEqualByComparingTo(BigDecimal("129000")) },
            )
        }

        @DisplayName("캐시가 없으면, null이 반환된다.")
        @Test
        fun returnsNull_whenCacheMiss() {
            // act
            val result = productCacheRepository.getProductDetail(999L)

            // assert
            assertThat(result).isNull()
        }

        @DisplayName("삭제 후 조회하면, null이 반환된다.")
        @Test
        fun returnsNull_whenEvicted() {
            // arrange
            val productId = 1L
            val info = createProductInfo(id = productId)
            productCacheRepository.setProductDetail(productId, info)

            // act
            productCacheRepository.evictProductDetail(productId)
            val result = productCacheRepository.getProductDetail(productId)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("상품 목록 캐시")
    @Nested
    inner class ProductListCache {

        @DisplayName("저장 후 조회하면, 동일한 데이터가 반환된다.")
        @Test
        fun returnsData_whenCacheHit() {
            // arrange
            val info1 = createProductInfo(id = 1L, name = "에어맥스 90")
            val info2 = createProductInfo(id = 2L, name = "에어포스 1")
            val cachedPage = CachedPage(
                content = listOf(info1, info2),
                page = 0,
                size = 20,
                totalElements = 2L,
            )

            // act
            productCacheRepository.setProductList(null, "createdAt: DESC", 0, 20, cachedPage)
            val result = productCacheRepository.getProductList(null, "createdAt: DESC", 0, 20)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result!!.content).hasSize(2) },
                { assertThat(result!!.totalElements).isEqualTo(2L) },
            )
        }

        @DisplayName("캐시가 없으면, null이 반환된다.")
        @Test
        fun returnsNull_whenCacheMiss() {
            // act
            val result = productCacheRepository.getProductList(null, "createdAt: DESC", 0, 20)

            // assert
            assertThat(result).isNull()
        }

        @DisplayName("전체 삭제 후 조회하면, null이 반환된다.")
        @Test
        fun returnsNull_whenAllEvicted() {
            // arrange
            val info = createProductInfo()
            val cachedPage = CachedPage(
                content = listOf(info),
                page = 0,
                size = 20,
                totalElements = 1L,
            )
            productCacheRepository.setProductList(null, "createdAt: DESC", 0, 20, cachedPage)
            productCacheRepository.setProductList(1L, "price: ASC", 0, 20, cachedPage)

            // act
            productCacheRepository.evictAllProductLists()

            // assert
            assertAll(
                { assertThat(productCacheRepository.getProductList(null, "createdAt: DESC", 0, 20)).isNull() },
                { assertThat(productCacheRepository.getProductList(1L, "price: ASC", 0, 20)).isNull() },
            )
        }
    }
}
