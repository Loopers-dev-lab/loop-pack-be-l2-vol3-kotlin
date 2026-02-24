package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderItemTest {

    @DisplayName("주문 항목 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrderItem_whenValidValuesProvided() {
            // arrange & act
            val orderItem = OrderItem(
                orderId = 1L,
                productId = 1L,
                quantity = 2,
                productName = "에어맥스",
                productPrice = 159000L,
                brandName = "나이키",
            )

            // assert
            assertAll(
                { assertThat(orderItem.orderId).isEqualTo(1L) },
                { assertThat(orderItem.productId).isEqualTo(1L) },
                { assertThat(orderItem.quantity).isEqualTo(2) },
                { assertThat(orderItem.productName).isEqualTo("에어맥스") },
                { assertThat(orderItem.productPrice).isEqualTo(159000L) },
                { assertThat(orderItem.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("수량이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZeroOrNegative() {
            // act
            val exception = assertThrows<CoreException> {
                OrderItem(
                    orderId = 1L,
                    productId = 1L,
                    quantity = 0,
                    productName = "에어맥스",
                    productPrice = 159000L,
                    brandName = "나이키",
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
