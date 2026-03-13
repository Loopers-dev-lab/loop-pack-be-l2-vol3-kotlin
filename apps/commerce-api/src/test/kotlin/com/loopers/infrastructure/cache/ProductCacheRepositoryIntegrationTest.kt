package com.loopers.infrastructure.cache

import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class ProductCacheRepositoryIntegrationTest @Autowired constructor(
    private val productCacheRepository: ProductCacheRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        redisCleanUp.truncateAll()
    }

    private fun createCacheDto(productId: Long = 1L): ProductCacheDto {
        return ProductCacheDto(
            id = productId,
            brandId = 10L,
            brandName = "TestBrand",
            brandDescription = "Test Brand Description",
            name = "TestProduct",
            description = "Test Product Description",
            price = 10000L,
            stockQuantity = 100,
            likeCount = 50,
        )
    }

    @DisplayName("상품 캐시를 조회할 때, ")
    @Nested
    inner class Get {
        @DisplayName("캐시가 없으면, null을 반환한다.")
        @Test
        fun returnsNull_whenCacheMiss() {
            // act
            val result = productCacheRepository.get(999L)

            // assert
            assertThat(result).isNull()
        }

        @DisplayName("캐시가 있으면, 캐시된 데이터를 반환한다.")
        @Test
        fun returnsCachedData_whenCacheHit() {
            // arrange
            val cacheDto = createCacheDto()
            productCacheRepository.put(cacheDto.id, cacheDto)

            // act
            val result = productCacheRepository.get(cacheDto.id)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result!!.id).isEqualTo(cacheDto.id) },
                { assertThat(result!!.brandId).isEqualTo(cacheDto.brandId) },
                { assertThat(result!!.brandName).isEqualTo(cacheDto.brandName) },
                { assertThat(result!!.name).isEqualTo(cacheDto.name) },
                { assertThat(result!!.price).isEqualTo(cacheDto.price) },
                { assertThat(result!!.stockQuantity).isEqualTo(cacheDto.stockQuantity) },
                { assertThat(result!!.likeCount).isEqualTo(cacheDto.likeCount) },
            )
        }
    }

    @DisplayName("상품 캐시를 삭제할 때, ")
    @Nested
    inner class Evict {
        @DisplayName("해당 상품의 캐시만 삭제된다.")
        @Test
        fun evictsOnlyTargetProduct() {
            // arrange
            val cacheDto1 = createCacheDto(productId = 1L)
            val cacheDto2 = createCacheDto(productId = 2L)
            productCacheRepository.put(cacheDto1.id, cacheDto1)
            productCacheRepository.put(cacheDto2.id, cacheDto2)

            // act
            productCacheRepository.evict(1L)

            // assert
            assertAll(
                { assertThat(productCacheRepository.get(1L)).isNull() },
                { assertThat(productCacheRepository.get(2L)).isNotNull() },
            )
        }
    }
}
