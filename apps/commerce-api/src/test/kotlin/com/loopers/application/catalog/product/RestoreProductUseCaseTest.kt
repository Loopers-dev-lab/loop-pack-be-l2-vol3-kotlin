package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class RestoreProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: RestoreProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = RestoreProductUseCase(productRepository)
    }

    @Nested
    @DisplayName("상품 복구 시")
    inner class Execute {

        @Test
        @DisplayName("삭제된 상품을 복구하면 deletedAt이 null이 된다")
        fun restoreProduct_deletedProduct_restoresSuccessfully() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = BrandId(1), name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )
            product.delete()
            productRepository.save(product)

            // act
            val result = useCase.execute(product.id.value)

            // assert
            assertThat(result.deletedAt).isNull()
            assertThat(result.name).isEqualTo("에어맥스 90")
        }

        @Test
        @DisplayName("삭제되지 않은 상품을 복구해도 정상 동작한다 (멱등)")
        fun restoreProduct_activeProduct_isIdempotent() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = BrandId(1), name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = Stock(100)),
            )

            // act
            val result = useCase.execute(product.id.value)

            // assert
            assertThat(result.deletedAt).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 상품을 복구하면 NOT_FOUND 예외가 발생한다")
        fun restoreProduct_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
