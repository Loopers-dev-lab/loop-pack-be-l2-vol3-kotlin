package com.loopers.application.admin.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
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
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("AdminProductRegisterUseCase")
class AdminProductRegisterUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val productStockRepository: ProductStockRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminProductRegisterUseCase(productRepository, productStockRepository, brandRepository)

    private fun command(
        name: String = "테스트 상품",
        regularPrice: BigDecimal = BigDecimal("10000.00"),
        sellingPrice: BigDecimal = BigDecimal("8000.00"),
        brandId: Long = 1L,
        initialStock: Int = 100,
        admin: String = "loopers.admin",
    ): AdminProductCommand.Register = AdminProductCommand.Register(
        name = name,
        regularPrice = regularPrice,
        sellingPrice = sellingPrice,
        brandId = brandId,
        initialStock = initialStock,
        imageUrl = null,
        thumbnailUrl = null,
        admin = admin,
    )

    private fun activeBrand(id: Long = 1L): Brand = Brand.retrieve(
        id = id,
        name = "테스트 브랜드",
        status = Brand.Status.ACTIVE,
    )

    @Nested
    @DisplayName("브랜드가 ACTIVE 상태이면 상품 등록에 성공한다")
    inner class WhenBrandActive {
        @Test
        @DisplayName("Product와 ProductStock을 저장하고 결과를 반환한다")
        fun register_success() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(activeBrand())
            given(productRepository.save(any(), any())).willAnswer {
                val product = it.arguments[0] as Product
                Product.retrieve(
                    id = 1L,
                    name = product.name,
                    regularPrice = product.regularPrice,
                    sellingPrice = product.sellingPrice,
                    brandId = product.brandId,
                    imageUrl = product.imageUrl,
                    thumbnailUrl = product.thumbnailUrl,
                    likeCount = product.likeCount,
                    status = product.status,
                )
            }
            given(productStockRepository.save(any(), any())).willAnswer {
                val stock = it.arguments[0] as ProductStock
                ProductStock.retrieve(id = 1L, productId = stock.productId, quantity = stock.quantity)
            }

            // act
            val result = useCase.register(command())

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(1L) },
                { assertThat(result.name).isEqualTo("테스트 상품") },
                { assertThat(result.stockQuantity).isEqualTo(100) },
                { assertThat(result.status).isEqualTo("INACTIVE") },
            )
        }

        @Test
        @DisplayName("ProductStock은 productId와 initialStock으로 생성된다")
        fun register_stockCreatedCorrectly() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(activeBrand())
            given(productRepository.save(any(), any())).willAnswer {
                val product = it.arguments[0] as Product
                Product.retrieve(
                    id = 5L,
                    name = product.name,
                    regularPrice = product.regularPrice,
                    sellingPrice = product.sellingPrice,
                    brandId = product.brandId,
                    imageUrl = product.imageUrl,
                    thumbnailUrl = product.thumbnailUrl,
                    likeCount = product.likeCount,
                    status = product.status,
                )
            }
            given(productStockRepository.save(any(), any())).willAnswer {
                val stock = it.arguments[0] as ProductStock
                ProductStock.retrieve(id = 1L, productId = stock.productId, quantity = stock.quantity)
            }

            // act
            useCase.register(command(initialStock = 50))

            // assert
            then(productStockRepository).should().save(
                check { stock ->
                    assertAll(
                        { assertThat(stock.productId).isEqualTo(5L) },
                        { assertThat(stock.quantity).isEqualTo(Quantity(50)) },
                    )
                },
                any(),
            )
        }
    }

    @Nested
    @DisplayName("브랜드가 존재하지 않으면 등록에 실패한다")
    inner class WhenBrandNotFound {
        @Test
        @DisplayName("BRAND_NOT_FOUND 예외를 던진다")
        fun register_brandNotFound() {
            // arrange
            given(brandRepository.findById(1L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> { useCase.register(command()) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("브랜드가 INACTIVE 상태이면 등록에 실패한다")
    inner class WhenBrandInactive {
        @Test
        @DisplayName("BRAND_INVALID_STATUS 예외를 던진다")
        fun register_brandInactive() {
            // arrange
            val inactiveBrand = Brand.retrieve(id = 1L, name = "브랜드", status = Brand.Status.INACTIVE)
            given(brandRepository.findById(1L)).willReturn(inactiveBrand)

            // act & assert
            val exception = assertThrows<CoreException> { useCase.register(command()) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_STATUS)
        }
    }
}
