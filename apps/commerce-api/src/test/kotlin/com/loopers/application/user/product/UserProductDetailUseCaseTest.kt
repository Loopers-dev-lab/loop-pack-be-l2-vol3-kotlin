package com.loopers.application.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("UserProductDetailUseCase")
class UserProductDetailUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val useCase = UserProductDetailUseCase(productRepository, productStockRepository, brandRepository)

    private fun activeProduct(id: Long = 1L, brandId: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "테스트 상품",
        regularPrice = Money(BigDecimal("10000.00")),
        sellingPrice = Money(BigDecimal("8000.00")),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 5,
        status = Product.Status.ACTIVE,
    )

    private fun inactiveProduct(id: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "비활성 상품",
        regularPrice = Money(BigDecimal("10000.00")),
        sellingPrice = Money(BigDecimal("8000.00")),
        brandId = 1L,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = Product.Status.INACTIVE,
    )

    private fun activeBrand(id: Long = 1L): Brand = Brand.retrieve(
        id = id,
        name = "테스트 브랜드",
        status = Brand.Status.ACTIVE,
    )

    private fun stock(productId: Long = 1L): ProductStock = ProductStock.retrieve(
        id = 1L,
        productId = productId,
        quantity = Quantity(10),
    )

    @Nested
    @DisplayName("상품이 ACTIVE이고 브랜드도 ACTIVE이면 상세 조회에 성공한다")
    inner class WhenActiveProductAndBrand {
        @Test
        @DisplayName("상품, 브랜드, 재고 정보를 조합하여 반환한다")
        fun getDetail_success() {
            // arrange
            given(productRepository.findById(1L)).willReturn(activeProduct())
            given(brandRepository.findById(1L)).willReturn(activeBrand())
            given(productStockRepository.findByProductId(1L)).willReturn(stock())

            // act
            val result = useCase.getDetail(1L)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(1L) },
                { assertThat(result.name).isEqualTo("테스트 상품") },
                { assertThat(result.brandName).isEqualTo("테스트 브랜드") },
                { assertThat(result.stockQuantity).isEqualTo(10) },
                { assertThat(result.likeCount).isEqualTo(5) },
            )
        }
    }

    @Nested
    @DisplayName("상품이 존재하지 않으면 조회에 실패한다")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("PRODUCT_NOT_FOUND 예외를 던진다")
        fun getDetail_productNotFound() {
            // arrange
            given(productRepository.findById(1L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("상품이 INACTIVE이면 조회에 실패한다")
    inner class WhenProductInactive {
        @Test
        @DisplayName("PRODUCT_NOT_FOUND 예외를 던진다")
        fun getDetail_productInactive() {
            // arrange
            given(productRepository.findById(1L)).willReturn(inactiveProduct())

            // act & assert
            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("브랜드가 INACTIVE이면 조회에 실패한다")
    inner class WhenBrandInactive {
        @Test
        @DisplayName("PRODUCT_NOT_FOUND 예외를 던진다")
        fun getDetail_brandInactive() {
            // arrange
            given(productRepository.findById(1L)).willReturn(activeProduct())
            val inactiveBrand = Brand.retrieve(id = 1L, name = "브랜드", status = Brand.Status.INACTIVE)
            given(brandRepository.findById(1L)).willReturn(inactiveBrand)

            // act & assert
            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }
}
