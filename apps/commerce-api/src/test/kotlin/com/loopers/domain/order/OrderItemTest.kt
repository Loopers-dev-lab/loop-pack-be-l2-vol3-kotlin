package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderItemTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_정보로_주문항목을_생성할_수_있다`() {
            // act
            val orderItem = OrderItem(
                productId = 1L,
                productName = "상품명",
                productPrice = 10000L,
                quantity = 2,
            )

            // assert
            assertAll(
                { assertThat(orderItem.productId).isEqualTo(1L) },
                { assertThat(orderItem.productName).isEqualTo("상품명") },
                { assertThat(orderItem.productPrice).isEqualTo(10000L) },
                { assertThat(orderItem.quantity).isEqualTo(2) },
                { assertThat(orderItem.subtotal).isEqualTo(20000L) },
            )
        }

        @Test
        fun `수량이_1이면_생성할_수_있다`() {
            val orderItem = OrderItem(
                productId = 1L,
                productName = "상품명",
                productPrice = 10000L,
                quantity = 1,
            )
            assertThat(orderItem.quantity).isEqualTo(1)
        }

        @Test
        fun `수량이_0이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> {
                OrderItem(
                    productId = 1L,
                    productName = "상품명",
                    productPrice = 10000L,
                    quantity = 0,
                )
            }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_ORDER_QUANTITY)
        }

        @Test
        fun `수량이_음수면_예외가_발생한다`() {
            val result = assertThrows<CoreException> {
                OrderItem(
                    productId = 1L,
                    productName = "상품명",
                    productPrice = 10000L,
                    quantity = -1,
                )
            }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_ORDER_QUANTITY)
        }
    }
}
