package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.repository.ProductCacheRepository
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductCacheRepositoryImplTest @Autowired constructor(
    private val productCacheRepository: ProductCacheRepository,
    private val redisCleanUp: RedisCleanUp,
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
            assertThat(found.name).isEqualTo("에어맥스 90")
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
        @DisplayName("brandId를 지정하여 evictProductList를 호출하면 예외가 발생하지 않는다")
        fun evictList_withBrandId_noException() {
            // act & assert (예외 없음)
            productCacheRepository.evictProductList(BrandId(10L))
        }

        @Test
        @DisplayName("brandId가 null인 경우 evictProductList를 호출하면 예외가 발생하지 않는다")
        fun evictList_withNullBrandId_noException() {
            // act & assert (예외 없음)
            productCacheRepository.evictProductList(null)
        }
    }
}
