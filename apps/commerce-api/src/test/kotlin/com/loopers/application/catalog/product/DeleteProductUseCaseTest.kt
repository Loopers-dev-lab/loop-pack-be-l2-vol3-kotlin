package com.loopers.application.catalog.product

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class DeleteProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: DeleteProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = DeleteProductUseCase(productRepository)
    }

    @Nested
    @DisplayName("상품 삭제 시")
    inner class Execute {

        @Test
        @DisplayName("상품을 삭제하면 soft delete된다")
        fun deleteProduct_softDeletes() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = 1L, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )

            // act
            useCase.execute(product.id)

            // assert
            val deleted = productRepository.findById(product.id)
            assertThat(deleted?.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("이미 삭제된 상품을 삭제하면 NOT_FOUND 예외가 발생한다")
        fun deleteProduct_alreadyDeleted_throwsNotFound() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = 1L, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )
            product.delete()
            productRepository.save(product)

            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(product.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제해도 예외가 발생하지 않는다 (멱등)")
        fun deleteProduct_nonExistent_isIdempotent() {
            // act & assert
            useCase.execute(999L)
        }
    }
}
