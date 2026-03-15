package com.loopers.application.admin.brand

import com.loopers.application.event.product.ProductQueryChangedEvent
import com.loopers.application.event.product.ProductQueryChangedEventPublisher
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
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("AdminBrandDeleteUseCase")
class AdminBrandDeleteUseCaseTest {
    private val productQueryChangedEventPublisher: ProductQueryChangedEventPublisher = mock()
    private val brandRepository: BrandRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val useCase = AdminBrandDeleteUseCase(
        brandRepository,
        productRepository,
        productStockRepository,
        productQueryChangedEventPublisher,
    )

    @Nested
    @DisplayName("브랜드 삭제 시")
    inner class WhenDelete {
        @Test
        @DisplayName("연관 상품이 없으면 브랜드만 삭제한다")
        fun delete_withoutProducts() {
            given(brandRepository.findById(1L)).willReturn(
                Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE),
            )
            given(productRepository.findAllByBrandId(1L)).willReturn(emptyList())

            useCase.delete(1L, "loopers.admin")

            then(brandRepository).should().delete(1L, "loopers.admin")
            then(productQueryChangedEventPublisher).should().publish(
                eq(ProductQueryChangedEvent(productIds = emptyList(), brandIds = listOf(1L))),
            )
            verifyNoInteractions(productStockRepository)
        }

        @Test
        @DisplayName("연관 상품이 있으면 상품과 재고를 삭제하고 이벤트를 발행한다")
        fun delete_withProducts() {
            given(brandRepository.findById(1L)).willReturn(
                Brand.retrieve(id = 1L, name = "나이키", status = Brand.Status.ACTIVE),
            )
            given(productRepository.findAllByBrandId(1L)).willReturn(
                listOf(
                    product(id = 101L, brandId = 1L),
                    product(id = 102L, brandId = 1L),
                ),
            )

            useCase.delete(1L, "loopers.admin")

            then(productStockRepository).should().deleteAllByProductIds(listOf(101L, 102L), "loopers.admin")
            then(productRepository).should().deleteAllByBrandId(1L, "loopers.admin")
            then(productQueryChangedEventPublisher).should().publish(
                eq(ProductQueryChangedEvent(productIds = listOf(101L, 102L), brandIds = listOf(1L))),
            )
            then(brandRepository).should().delete(1L, "loopers.admin")
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 실패한다")
    inner class WhenNotFound {
        @Test
        @DisplayName("CoreException(BRAND_NOT_FOUND)을 던진다")
        fun delete_notFound() {
            given(brandRepository.findById(999L)).willReturn(null)

            val exception = assertThrows<CoreException> {
                useCase.delete(999L, "loopers.admin")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
            verifyNoInteractions(productRepository, productStockRepository, productQueryChangedEventPublisher)
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
