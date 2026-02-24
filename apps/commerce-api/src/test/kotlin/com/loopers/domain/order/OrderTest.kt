package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderTest {

    @DisplayName("주문 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrder_whenValidValuesProvided() {
            // arrange & act
            val order = Order(
                userId = 1L,
                totalAmount = 318000L,
            )

            // assert
            assertAll(
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.totalAmount).isEqualTo(318000L) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @DisplayName("총 금액이 0 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenTotalAmountIsNegative() {
            // act
            val exception = assertThrows<CoreException> {
                Order(userId = 1L, totalAmount = -1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
