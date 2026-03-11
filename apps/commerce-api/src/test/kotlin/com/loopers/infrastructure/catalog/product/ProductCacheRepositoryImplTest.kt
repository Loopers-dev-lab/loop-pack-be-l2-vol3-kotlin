package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.config.redis.RedisConfig.Companion.REDIS_TEMPLATE_MASTER
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCacheRepositoryImplTest @Autowired constructor(
    private val productCacheRepository: ProductCacheRepository,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(REDIS_TEMPLATE_MASTER) private val redisTemplateMaster: RedisTemplate<String, String>,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun product(id: Long = 1L) = Product(
        id = ProductId(id),
        refBrandId = BrandId(10L),
        name = "에어맥스 90",
        price = Money(BigDecimal("129000")),
        stock = Stock(100),
    )

    @Nested
    @DisplayName("saveProductDetail / findProductDetail")
    inner class SaveAndFind {

        @Test
        @DisplayName("저장한 상품을 productId로 조회하면 동일한 상품이 반환된다")
        fun saveAndFind_returnsSameProduct() {
            // arrange
            val saved = product()

            // act
            productCacheRepository.saveProductDetail(saved)
            val found = productCacheRepository.findProductDetail(ProductId(1L))

            // assert
            assertThat(found).isNotNull
            assertThat(found!!.id).isEqualTo(ProductId(1L))
            assertThat(found.refBrandId).isEqualTo(BrandId(10L))
            assertThat(found.name).isEqualTo("에어맥스 90")
            assertThat(found.price).isEqualTo(Money(BigDecimal("129000")))
            assertThat(found.stock).isEqualTo(Stock(100))
            assertThat(found.status).isEqualTo(Product.ProductStatus.ON_SALE)
            assertThat(found.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("저장하지 않은 productId로 조회하면 null이 반환된다")
        fun find_nonExistent_returnsNull() {
            // act
            val found = productCacheRepository.findProductDetail(ProductId(999L))

            // assert
            assertThat(found).isNull()
        }
    }

    @Nested
    @DisplayName("findProductDetail — 역직렬화 실패 처리")
    inner class FindDeserializationFailure {

        @Test
        @DisplayName("손상된 JSON이 캐시에 있으면 null을 반환하고 해당 키를 삭제한다")
        fun find_corruptedJson_returnsNullAndDeletesKey() {
            // arrange — 손상된 JSON을 직접 삽입
            val key = "product:detail:1"
            redisTemplateMaster.opsForValue().set(key, "{invalid-json}")

            // act
            val result = productCacheRepository.findProductDetail(ProductId(1L))

            // assert
            assertThat(result).isNull()
            assertThat(redisTemplateMaster.hasKey(key)).isFalse()
        }
    }

    @Nested
    @DisplayName("evictProductDetail")
    inner class EvictDetail {

        @Test
        @DisplayName("evict 후 조회하면 null이 반환된다")
        fun evict_thenFind_returnsNull() {
            // arrange
            val saved = product()
            productCacheRepository.saveProductDetail(saved)

            // act
            productCacheRepository.evictProductDetail(ProductId(1L))
            val found = productCacheRepository.findProductDetail(ProductId(1L))

            // assert
            assertThat(found).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 productId를 evict해도 예외가 발생하지 않는다")
        fun evict_nonExistent_noException() {
            // act & assert (예외 없음)
            productCacheRepository.evictProductDetail(ProductId(999L))
        }
    }

    @Nested
    @DisplayName("evictProductList")
    inner class EvictList {

        @Test
        @DisplayName("brandId를 지정하여 evictProductList를 호출하면 해당 브랜드의 목록 캐시가 삭제된다")
        fun evictList_withBrandId_deletesMatchingKeys() {
            // arrange — Spring Cache 형식(product:list::key)으로 키 저장
            redisTemplateMaster.opsForValue().set("product:list::10:LATEST:0:20", "cached-data")
            redisTemplateMaster.opsForValue().set("product:list::all:LATEST:0:20", "cached-data")
            redisTemplateMaster.opsForValue().set("product:list::99:LATEST:0:20", "should-remain")

            // act
            productCacheRepository.evictProductList(BrandId(10L))

            // assert — brandId=10 과 all 키는 삭제, brandId=99 키는 유지
            assertThat(redisTemplateMaster.hasKey("product:list::10:LATEST:0:20")).isFalse()
            assertThat(redisTemplateMaster.hasKey("product:list::all:LATEST:0:20")).isFalse()
            assertThat(redisTemplateMaster.hasKey("product:list::99:LATEST:0:20")).isTrue()
        }

        @Test
        @DisplayName("brandId가 null이면 모든 목록 캐시가 삭제된다")
        fun evictList_withNullBrandId_deletesAllListKeys() {
            // arrange
            redisTemplateMaster.opsForValue().set("product:list::10:LATEST:0:20", "cached-data")
            redisTemplateMaster.opsForValue().set("product:list::all:LATEST:0:20", "cached-data")

            // act
            productCacheRepository.evictProductList(null)

            // assert
            assertThat(redisTemplateMaster.hasKey("product:list::10:LATEST:0:20")).isFalse()
            assertThat(redisTemplateMaster.hasKey("product:list::all:LATEST:0:20")).isFalse()
        }
    }
}
