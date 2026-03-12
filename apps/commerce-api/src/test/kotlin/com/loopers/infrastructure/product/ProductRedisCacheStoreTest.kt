package com.loopers.infrastructure.product

import com.loopers.application.product.ProductInfo
import com.loopers.application.product.ProductListCache
import com.loopers.domain.product.ProductSortType
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime

@SpringBootTest
class ProductRedisCacheStoreTest @Autowired constructor(
    private val productRedisCacheStore: ProductRedisCacheStore,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun createProductInfo(
        id: Long = 1L,
        name: String = "테스트 상품",
    ): ProductInfo {
        val now = ZonedDateTime.now()
        return ProductInfo(
            id = id,
            brandId = 1L,
            brandName = "나이키",
            name = name,
            description = "상품 설명",
            price = 10000L,
            stock = 100,
            imageUrl = "https://example.com/image.jpg",
            likeCount = 5L,
            available = true,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
    }

    @DisplayName("상품 상세 캐시")
    @Nested
    inner class DetailCache {

        @DisplayName("캐시에 저장하고 조회하면 동일한 데이터가 반환된다")
        @Test
        fun putAndGet() {
            val productInfo = createProductInfo()

            productRedisCacheStore.putDetail(1L, productInfo)
            val cached = productRedisCacheStore.getDetail(1L)

            val result = requireNotNull(cached)
            assertAll(
                { assertThat(result.id).isEqualTo(1L) },
                { assertThat(result.name).isEqualTo("테스트 상품") },
                { assertThat(result.brandName).isEqualTo("나이키") },
                { assertThat(result.price).isEqualTo(10000L) },
                { assertThat(result.likeCount).isEqualTo(5L) },
            )
        }

        @DisplayName("캐시가 없으면 null을 반환한다")
        @Test
        fun getReturnsNullWhenMiss() {
            val cached = productRedisCacheStore.getDetail(999L)

            assertThat(cached).isNull()
        }

        @DisplayName("캐시를 무효화하면 null을 반환한다")
        @Test
        fun evictDetail() {
            val productInfo = createProductInfo()
            productRedisCacheStore.putDetail(1L, productInfo)

            productRedisCacheStore.evictDetail(1L)

            assertThat(productRedisCacheStore.getDetail(1L)).isNull()
        }
    }

    @DisplayName("상품 목록 ID 캐시")
    @Nested
    inner class ListIdCache {

        @DisplayName("목록 ID 캐시에 저장하고 조회하면 동일한 데이터가 반환된다")
        @Test
        fun putAndGet() {
            val listCache = ProductListCache(
                productIds = listOf(1L, 2L, 3L),
                totalElements = 100L,
            )

            productRedisCacheStore.putListIds(null, ProductSortType.LATEST, 0, listCache)
            val cached = productRedisCacheStore.getListIds(null, ProductSortType.LATEST, 0)

            val result = requireNotNull(cached)
            assertAll(
                { assertThat(result.productIds).containsExactly(1L, 2L, 3L) },
                { assertThat(result.totalElements).isEqualTo(100L) },
            )
        }

        @DisplayName("브랜드 필터가 다르면 다른 캐시 키를 사용한다")
        @Test
        fun differentBrandIdUsesDifferentKey() {
            val cache1 = ProductListCache(productIds = listOf(1L), totalElements = 1L)
            val cache2 = ProductListCache(productIds = listOf(2L), totalElements = 1L)

            productRedisCacheStore.putListIds(null, ProductSortType.LATEST, 0, cache1)
            productRedisCacheStore.putListIds(1L, ProductSortType.LATEST, 0, cache2)

            val cached1 = requireNotNull(productRedisCacheStore.getListIds(null, ProductSortType.LATEST, 0))
            val cached2 = requireNotNull(productRedisCacheStore.getListIds(1L, ProductSortType.LATEST, 0))

            assertAll(
                { assertThat(cached1.productIds).containsExactly(1L) },
                { assertThat(cached2.productIds).containsExactly(2L) },
            )
        }

        @DisplayName("목록 캐시를 전체 무효화하면 모든 목록 캐시가 삭제된다")
        @Test
        fun evictAllLists() {
            val cache = ProductListCache(productIds = listOf(1L), totalElements = 1L)
            productRedisCacheStore.putListIds(null, ProductSortType.LATEST, 0, cache)
            productRedisCacheStore.putListIds(1L, ProductSortType.LIKE_COUNT, 0, cache)

            productRedisCacheStore.evictAllLists()

            assertAll(
                { assertThat(productRedisCacheStore.getListIds(null, ProductSortType.LATEST, 0)).isNull() },
                { assertThat(productRedisCacheStore.getListIds(1L, ProductSortType.LIKE_COUNT, 0)).isNull() },
            )
        }

        @DisplayName("목록 캐시 무효화는 상세 캐시에 영향을 주지 않는다")
        @Test
        fun evictAllListsDoesNotAffectDetail() {
            val productInfo = createProductInfo()
            productRedisCacheStore.putDetail(1L, productInfo)

            val listCache = ProductListCache(productIds = listOf(1L), totalElements = 1L)
            productRedisCacheStore.putListIds(null, ProductSortType.LATEST, 0, listCache)

            productRedisCacheStore.evictAllLists()

            assertThat(productRedisCacheStore.getDetail(1L)).isNotNull
        }
    }

    @DisplayName("상세 캐시 일괄 조회")
    @Nested
    inner class BatchDetailCache {

        @DisplayName("여러 상품의 상세 캐시를 한번에 조회할 수 있다")
        @Test
        fun getDetails() {
            productRedisCacheStore.putDetail(1L, createProductInfo(1L, "상품1"))
            productRedisCacheStore.putDetail(2L, createProductInfo(2L, "상품2"))

            val details = productRedisCacheStore.getDetails(listOf(1L, 2L, 3L))

            assertAll(
                { assertThat(details).hasSize(2) },
                { assertThat(details[1L]?.name).isEqualTo("상품1") },
                { assertThat(details[2L]?.name).isEqualTo("상품2") },
                { assertThat(details[3L]).isNull() },
            )
        }

        @DisplayName("빈 목록을 조회하면 빈 맵을 반환한다")
        @Test
        fun getDetailsWithEmptyList() {
            val details = productRedisCacheStore.getDetails(emptyList())

            assertThat(details).isEmpty()
        }
    }
}
