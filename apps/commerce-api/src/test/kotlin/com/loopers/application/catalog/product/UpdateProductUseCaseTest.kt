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

class UpdateProductUseCaseTest {

    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: UpdateProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = FakeProductRepository()
        useCase = UpdateProductUseCase(productRepository)
    }

    private fun createProduct(
        name: String = "에어맥스 90",
        price: BigDecimal = BigDecimal("129000"),
        stock: Int = 100,
    ): Product {
        return productRepository.save(
            Product(refBrandId = 1L, name = name, price = Money(price), stock = stock),
        )
    }

    @Nested
    @DisplayName("상품 수정 시")
    inner class Execute {

        @Test
        @DisplayName("유효한 정보로 수정하면 상품이 변경된다")
        fun updateProduct_withValidData_updatesProduct() {
            // arrange
            val product = createProduct()

            // act
            val result = useCase.execute(product.id, "에어맥스 95", BigDecimal("159000"), 50, null)

            // assert
            assertThat(result.name).isEqualTo("에어맥스 95")
            assertThat(result.price).isEqualByComparingTo(BigDecimal("159000"))
            assertThat(result.stock).isEqualTo(50)
        }

        @Test
        @DisplayName("삭제된 상품도 수정할 수 있다")
        fun updateProduct_deletedProduct_succeeds() {
            // arrange
            val product = createProduct()
            product.delete()
            productRepository.save(product)

            // act
            val result = useCase.execute(product.id, "변경", null, null, null)

            // assert
            assertThat(result.name).isEqualTo("변경")
        }

        @Test
        @DisplayName("모든 필드가 null이면 BAD_REQUEST 예외가 발생한다")
        fun updateProduct_allFieldsNull_throwsBadRequest() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                useCase.execute(1L, null, null, null, null)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("수정할 항목이 최소 1개 이상 필요합니다.")
        }

        @Test
        @DisplayName("HIDDEN 상품도 어드민이 수정할 수 있다")
        fun updateProduct_hiddenProduct_success() {
            // arrange
            val product = createProduct()
            useCase.execute(product.id, null, null, null, "HIDDEN")

            // act
            val updated = useCase.execute(product.id, "변경", null, null, null)

            // assert
            assertThat(updated.name).isEqualTo("변경")
            assertThat(updated.status).isEqualTo(Product.ProductStatus.HIDDEN.name)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 NOT_FOUND 예외가 발생한다")
        fun updateProduct_nonExistent_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                useCase.execute(999L, "변경", null, null, null)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
