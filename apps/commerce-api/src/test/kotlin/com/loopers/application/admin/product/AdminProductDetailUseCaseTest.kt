package com.loopers.application.admin.product

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

@DisplayName("AdminProductDetailUseCase")
class AdminProductDetailUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminProductDetailUseCase(productRepository, productStockRepository, brandRepository)

    private fun product(id: Long = 1L, brandId: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "상품",
        regularPrice = Money(BigDecimal("10000")),
        sellingPrice = Money(BigDecimal("8000")),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 5,
        status = Product.Status.ACTIVE,
    )

    private fun brand(id: Long = 1L): Brand = Brand.retrieve(id = id, name = "브랜드", status = Brand.Status.ACTIVE)

    private fun stock(productId: Long = 1L): ProductStock =
        ProductStock.retrieve(id = 1L, productId = productId, quantity = Quantity(100))

    @Nested
    @DisplayName("Product, Brand, Stock이 모두 존재하면 상세 정보를 반환한다")
    inner class WhenAllExist {
        @Test
        @DisplayName("Product + Brand + Stock 정보가 조합된 Detail을 반환한다")
        fun getDetail_success() {
            given(productRepository.findById(1L)).willReturn(product())
            given(brandRepository.findById(1L)).willReturn(brand())
            given(productStockRepository.findByProductId(1L)).willReturn(stock())

            val result = useCase.getDetail(1L)

            assertAll(
                { assertThat(result.name).isEqualTo("상품") },
                { assertThat(result.brandName).isEqualTo("브랜드") },
                { assertThat(result.stockQuantity).isEqualTo(100) },
                { assertThat(result.likeCount).isEqualTo(5) },
            )
        }
    }

    @Nested
    @DisplayName("상품이 존재하지 않으면 PRODUCT_NOT_FOUND 예외를 던진다")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("PRODUCT_NOT_FOUND")
        fun getDetail_productNotFound() {
            given(productRepository.findById(1L)).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 BRAND_NOT_FOUND 예외를 던진다")
    inner class WhenBrandNotFound {
        @Test
        @DisplayName("BRAND_NOT_FOUND")
        fun getDetail_brandNotFound() {
            given(productRepository.findById(1L)).willReturn(product())
            given(brandRepository.findById(1L)).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("재고가 존재하지 않으면 PRODUCT_STOCK_NOT_FOUND 예외를 던진다")
    inner class WhenStockNotFound {
        @Test
        @DisplayName("PRODUCT_STOCK_NOT_FOUND")
        fun getDetail_stockNotFound() {
            given(productRepository.findById(1L)).willReturn(product())
            given(brandRepository.findById(1L)).willReturn(brand())
            given(productStockRepository.findByProductId(1L)).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.getDetail(1L) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_STOCK_NOT_FOUND)
        }
    }
}
