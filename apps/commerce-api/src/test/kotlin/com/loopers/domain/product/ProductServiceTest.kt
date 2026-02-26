package com.loopers.domain.product

import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductService(productRepository)
    }

    @DisplayName("상품 단건 조회할 때,")
    @Nested
    inner class GetProduct {

        @DisplayName("유효한 productId를 전달하면, 상품을 반환한다.")
        @Test
        fun returnsProduct_whenValidProductIdProvided() {
            // arrange
            val productId = 1L
            val product = Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                likes = LikeCount.of(10),
                stockQuantity = StockQuantity.of(100),
                brandId = 1L,
            )

            whenever(productRepository.findById(productId)).thenReturn(product)

            // act
            val result = productService.getProduct(productId)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("에어맥스") },
                { assertThat(result.price).isEqualTo(Money.of(159000L)) },
            )
        }

        @DisplayName("존재하지 않는 productId를 전달하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // arrange
            val productId = 999L

            whenever(productRepository.findById(productId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                productService.getProduct(productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
