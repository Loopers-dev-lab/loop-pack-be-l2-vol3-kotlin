package com.loopers.infrastructure.product

import com.loopers.application.product.ProductInfo
import com.loopers.support.PageResult
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

            assertAll(
                { assertThat(cached).isNotNull },
                { assertThat(cached!!.id).isEqualTo(1L) },
                { assertThat(cached!!.name).isEqualTo("테스트 상품") },
                { assertThat(cached!!.brandName).isEqualTo("나이키") },
                { assertThat(cached!!.price).isEqualTo(10000L) },
                { assertThat(cached!!.likeCount).isEqualTo(5L) },
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

    @DisplayName("상품 목록 캐시")
    @Nested
    inner class ListCache {

        @DisplayName("목록 캐시에 저장하고 조회하면 동일한 데이터가 반환된다")
        @Test
        fun putAndGet() {
            val pageResult = PageResult.of(
                content = listOf(createProductInfo(1L, "상품1"), createProductInfo(2L, "상품2")),
                page = 0,
                size = 20,
                totalElements = 2L,
            )

            productRedisCacheStore.putList(null, "LATEST", 0, 20, pageResult)
            val cached = productRedisCacheStore.getList(null, "LATEST", 0, 20)

            assertAll(
                { assertThat(cached).isNotNull },
                { assertThat(cached!!.content).hasSize(2) },
                { assertThat(cached!!.totalElements).isEqualTo(2L) },
            )
        }

        @DisplayName("브랜드 필터가 다르면 다른 캐시 키를 사용한다")
        @Test
        fun differentBrandIdUsesDifferentKey() {
            val result1 = PageResult.of(
                content = listOf(createProductInfo(1L, "전체")),
                page = 0,
                size = 20,
                totalElements = 1L,
            )
            val result2 = PageResult.of(
                content = listOf(createProductInfo(2L, "브랜드1")),
                page = 0,
                size = 20,
                totalElements = 1L,
            )

            productRedisCacheStore.putList(null, "LATEST", 0, 20, result1)
            productRedisCacheStore.putList(1L, "LATEST", 0, 20, result2)

            val cached1 = productRedisCacheStore.getList(null, "LATEST", 0, 20)
            val cached2 = productRedisCacheStore.getList(1L, "LATEST", 0, 20)

            assertAll(
                { assertThat(cached1!!.content[0].name).isEqualTo("전체") },
                { assertThat(cached2!!.content[0].name).isEqualTo("브랜드1") },
            )
        }

        @DisplayName("목록 캐시를 전체 무효화하면 모든 목록 캐시가 삭제된다")
        @Test
        fun evictAllLists() {
            val result = PageResult.of(
                content = listOf(createProductInfo()),
                page = 0,
                size = 20,
                totalElements = 1L,
            )
            productRedisCacheStore.putList(null, "LATEST", 0, 20, result)
            productRedisCacheStore.putList(1L, "LIKE_COUNT", 0, 20, result)

            productRedisCacheStore.evictAllLists()

            assertAll(
                { assertThat(productRedisCacheStore.getList(null, "LATEST", 0, 20)).isNull() },
                { assertThat(productRedisCacheStore.getList(1L, "LIKE_COUNT", 0, 20)).isNull() },
            )
        }

        @DisplayName("목록 캐시 무효화는 상세 캐시에 영향을 주지 않는다")
        @Test
        fun evictAllListsDoesNotAffectDetail() {
            val productInfo = createProductInfo()
            productRedisCacheStore.putDetail(1L, productInfo)

            val listResult = PageResult.of(
                content = listOf(productInfo),
                page = 0,
                size = 20,
                totalElements = 1L,
            )
            productRedisCacheStore.putList(null, "LATEST", 0, 20, listResult)

            productRedisCacheStore.evictAllLists()

            assertThat(productRedisCacheStore.getDetail(1L)).isNotNull
        }
    }
}
