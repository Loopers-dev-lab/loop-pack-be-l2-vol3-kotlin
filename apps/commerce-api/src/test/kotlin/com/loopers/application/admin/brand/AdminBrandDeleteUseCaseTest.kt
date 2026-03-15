package com.loopers.application.admin.brand

import com.loopers.application.product.ProductQueryCache
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("AdminBrandDeleteUseCase")
class AdminBrandDeleteUseCaseTest {
    private val productQueryCache: ProductQueryCache = mock()
    private val brandRepository: BrandRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val useCase = AdminBrandDeleteUseCase(brandRepository, productRepository, productStockRepository, productQueryCache)

    @Nested
    @DisplayName("브랜드 삭제 시")
    inner class WhenDelete {
        @Test
        @DisplayName("연관 상품이 없으면 브랜드만 삭제한다")
        fun delete_withoutProducts() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(
                Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE),
            )
            given(productRepository.findAllByBrandId(1L)).willReturn(emptyList())

            // act
            useCase.delete(1L, "loopers.admin")

            // assert
            then(brandRepository).should().delete(1L, "loopers.admin")
            verifyNoInteractions(productStockRepository, productQueryCache)
        }

        @Test
        @DisplayName("연관 상품이 있으면 상품과 재고를 삭제하고 상세 캐시도 비운다")
        fun delete_withProducts() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(
                Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE),
            )
            given(productRepository.findAllByBrandId(1L)).willReturn(
                listOf(
                    product(id = 101L, brandId = 1L),
                    product(id = 102L, brandId = 1L),
                ),
            )

            // act
            useCase.delete(1L, "loopers.admin")

            // assert
            then(productStockRepository).should().deleteAllByProductIds(listOf(101L, 102L), "loopers.admin")
            then(productRepository).should().deleteAllByBrandId(1L, "loopers.admin")
            then(productQueryCache).should().evictDetails(listOf(101L, 102L))
            then(brandRepository).should().delete(1L, "loopers.admin")
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 실패한다")
    inner class WhenNotFound {
        @Test
        @DisplayName("CoreException(BRAND_NOT_FOUND)을 던진다")
        fun delete_notFound() {
            // arrange
            given(brandRepository.findById(999L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> {
                useCase.delete(999L, "loopers.admin")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
            verifyNoInteractions(productRepository, productStockRepository, productQueryCache)
        }
    }

    private fun product(id: Long, brandId: Long): Product = Product.retrieve(
        id = id,
        name = "상품$id",
        regularPrice = Money(BigDecimal("10000")),
        sellingPrice = Money(BigDecimal("9000")),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = Product.Status.ACTIVE,
    )
}
