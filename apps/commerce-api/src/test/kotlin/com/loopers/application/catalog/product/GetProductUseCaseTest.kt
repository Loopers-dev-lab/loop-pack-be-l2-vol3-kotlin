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

class GetProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: GetProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = GetProductUseCase(productRepository)
    }

    @Nested
    @DisplayName("어드민 상품 단건 조회 시")
    inner class ExecuteAdmin {

        @Test
        @DisplayName("삭제된 상품도 조회된다")
        fun executeAdmin_includesDeleted() {
            // arrange
            val product = productRepository.save(
                Product(refBrandId = 1L, name = "에어맥스 90", price = Money(BigDecimal("129000")), stock = 100),
            )
            product.delete()
            productRepository.save(product)

            // act
            val result = useCase.executeAdmin(product.id)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 90")
            assertThat(result.deletedAt).isNotNull()
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        fun executeAdmin_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.executeAdmin(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
