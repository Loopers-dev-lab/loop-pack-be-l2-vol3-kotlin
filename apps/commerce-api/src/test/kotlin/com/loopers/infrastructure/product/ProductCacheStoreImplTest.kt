package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.product.ProductCacheSnapshot
import com.loopers.domain.product.DisplayStatus
import com.loopers.domain.product.ProductSortType
import com.loopers.domain.product.SaleStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration
import java.time.ZonedDateTime

@DisplayName("ProductCacheStoreImpl")
class ProductCacheStoreImplTest {
    private val redisTemplate: RedisTemplate<String, String> = mockk()
    private val valueOperations: ValueOperations<String, String> = mockk(relaxed = true)
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()
    private val productCacheStore = ProductCacheStoreImpl(redisTemplate, objectMapper)
    private val cache = linkedMapOf<String, String>()

    init {
        every { redisTemplate.opsForValue() } returns valueOperations
        every { valueOperations.get(any()) } answers { cache[firstArg()] }
        every { valueOperations.set(any(), any(), any<Duration>()) } answers {
            cache[firstArg()] = secondArg()
        }
        every { redisTemplate.delete(any<String>()) } answers {
            cache.remove(firstArg()) != null
        }
        every { redisTemplate.keys(any()) } answers {
            val pattern = firstArg<String>().removeSuffix("*")
            cache.keys.filter { it.startsWith(pattern) }.toSet()
        }
        every { redisTemplate.delete(any<Collection<String>>()) } answers {
            firstArg<Collection<String>>().forEach(cache::remove)
            1L
        }
    }

    @DisplayName("상품 상세 캐시를 저장하고 다시 조회한다")
    @Test
    fun putAndGetProductDetail() {
        // arrange
        val snapshot = createSnapshot(productId = 11L)

        // act
        productCacheStore.putProductDetail(snapshot)
        val cached = productCacheStore.getProductDetail(11L)

        // assert
        assertThat(cached).isNotNull
        assertThat(cached?.id).isEqualTo(snapshot.id)
        assertThat(cached?.name).isEqualTo(snapshot.name)
        assertThat(cached?.brandId).isEqualTo(snapshot.brandId)
        assertThat(cached?.likesCount).isEqualTo(snapshot.likesCount)
        verify(exactly = 1) { valueOperations.set("product:detail:11", any(), Duration.ofMinutes(10)) }
    }

    @DisplayName("상품 목록 캐시를 전체 무효화한다")
    @Test
    fun evictsProductListKeys() {
        // arrange
        val pageable = PageRequest.of(0, 20)
        val snapshots = PageImpl(listOf(createSnapshot(21L), createSnapshot(22L)), pageable, 2)
        productCacheStore.putProductList(brandId = 3L, sortType = ProductSortType.LIKES_DESC, pageable = pageable, products = snapshots)

        // act
        val cached = productCacheStore.getProductList(brandId = 3L, sortType = ProductSortType.LIKES_DESC, pageable = pageable)
        productCacheStore.evictProductList()
        val evicted = productCacheStore.getProductList(brandId = 3L, sortType = ProductSortType.LIKES_DESC, pageable = pageable)

        // assert
        assertThat(cached?.content).hasSize(2)
        assertThat(evicted).isNull()
        verify(exactly = 1) { valueOperations.set("product:list:3:likes_desc:0:20", any(), Duration.ofMinutes(3)) }
    }

    private fun createSnapshot(productId: Long): ProductCacheSnapshot = ProductCacheSnapshot(
            id = productId,
            name = "상품$productId",
            price = 10000L + productId,
            brandId = 3L,
            description = "설명$productId",
            thumbnailImageUrl = "https://example.com/$productId.png",
            stockQuantity = 10,
            likesCount = productId,
            saleStatus = SaleStatus.SELLING,
            displayStatus = DisplayStatus.VISIBLE,
            createdAt = ZonedDateTime.now(),
        )
}
