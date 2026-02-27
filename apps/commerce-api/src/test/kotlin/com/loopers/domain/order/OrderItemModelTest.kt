package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class OrderItemModelTest {
    companion object {
        private const val DEFAULT_ORDER_ID = 1L
        private const val DEFAULT_PRODUCT_ID = 1L
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 2
        private val DEFAULT_PRICE = BigDecimal("129000")
    }

    private fun createOrderItemModel(
        orderId: Long = DEFAULT_ORDER_ID,
        productId: Long = DEFAULT_PRODUCT_ID,
        productName: String = DEFAULT_PRODUCT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_PRICE,
    ) = OrderItemModel(
        orderId = orderId,
        productId = productId,
        productName = productName,
        quantity = quantity,
        price = price,
    )

    @DisplayName("생성")
    @Nested
    inner class Create {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrderItemModelWhenValidParametersAreProvided() {
            // act
            val orderItem = createOrderItemModel()

            // assert
            assertAll(
                { assertThat(orderItem.orderId).isEqualTo(DEFAULT_ORDER_ID) },
                { assertThat(orderItem.productId).isEqualTo(DEFAULT_PRODUCT_ID) },
                { assertThat(orderItem.productName).isEqualTo(DEFAULT_PRODUCT_NAME) },
                { assertThat(orderItem.quantity).isEqualTo(DEFAULT_QUANTITY) },
                { assertThat(orderItem.price).isEqualByComparingTo(DEFAULT_PRICE) },
            )
        }

        @DisplayName("상품명이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenProductNameIsBlank() {
            // act & assert
            val result = assertThrows<CoreException> {
                createOrderItemModel(productName = "   ")
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 0이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityIsZero() {
            // act & assert
            val result = assertThrows<CoreException> {
                createOrderItemModel(quantity = 0)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenQuantityIsNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createOrderItemModel(quantity = -1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenPriceIsZeroOrNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createOrderItemModel(price = BigDecimal.ZERO)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
