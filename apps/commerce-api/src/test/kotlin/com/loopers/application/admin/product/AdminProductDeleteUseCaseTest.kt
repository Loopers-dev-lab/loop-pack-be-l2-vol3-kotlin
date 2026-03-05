package com.loopers.application.admin.product

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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("AdminProductDeleteUseCase")
class AdminProductDeleteUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val useCase = AdminProductDeleteUseCase(productRepository, productStockRepository)

    private val admin = "loopers.admin"

    private fun product(id: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "상품",
        regularPrice = Money(BigDecimal("10000")),
        sellingPrice = Money(BigDecimal("8000")),
        brandId = 1L,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = Product.Status.ACTIVE,
    )

    @Nested
    @DisplayName("상품이 존재하면 Stock과 Product를 삭제한다")
    inner class WhenProductExists {
        @Test
        @DisplayName("ProductStock 삭제 후 Product를 삭제한다")
        fun delete_success() {
            given(productRepository.findById(1L)).willReturn(product())

            useCase.delete(1L, admin)

            then(productStockRepository).should().deleteByProductId(eq(1L), eq(admin))
            then(productRepository).should().delete(eq(1L), eq(admin))
        }
    }

    @Nested
    @DisplayName("상품이 존재하지 않으면 삭제에 실패한다")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("PRODUCT_NOT_FOUND 예외를 던진다")
        fun delete_notFound() {
            given(productRepository.findById(1L)).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.delete(1L, admin) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }
}
