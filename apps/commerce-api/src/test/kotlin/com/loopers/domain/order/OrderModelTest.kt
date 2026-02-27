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

class OrderModelTest {
    companion object {
        private const val DEFAULT_USER_ID = 1L
        private val DEFAULT_TOTAL_PRICE = BigDecimal("258000")
    }

    private fun createOrderModel(
        userId: Long = DEFAULT_USER_ID,
        status: OrderStatus = OrderStatus.ORDERED,
        totalPrice: BigDecimal = DEFAULT_TOTAL_PRICE,
    ) = OrderModel(userId = userId, status = status, totalPrice = totalPrice)

    @DisplayName("생성")
    @Nested
    inner class Create {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrderModelWhenValidParametersAreProvided() {
            // act
            val order = createOrderModel()

            // assert
            assertAll(
                { assertThat(order.userId).isEqualTo(DEFAULT_USER_ID) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(order.totalPrice).isEqualByComparingTo(DEFAULT_TOTAL_PRICE) },
            )
        }

        @DisplayName("총 주문 금액이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenTotalPriceIsZeroOrNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createOrderModel(totalPrice = BigDecimal.ZERO)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("총 주문 금액이 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenTotalPriceIsNegative() {
            // act & assert
            val result = assertThrows<CoreException> {
                createOrderModel(totalPrice = BigDecimal("-1"))
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
