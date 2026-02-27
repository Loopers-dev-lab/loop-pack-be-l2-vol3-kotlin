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

class OrderDomainServiceTest {

    private val orderDomainService = OrderDomainService()

    @DisplayName("주문을 조립할 때,")
    @Nested
    inner class BuildOrder {

        @DisplayName("유효한 아이템이 주어지면, Order가 생성된다.")
        @Test
        fun buildsOrder_whenValidItemsProvided() {
            // arrange
            val items = listOf(
                OrderItemCommand(
                    productId = 1L,
                    productName = "에어맥스 90",
                    brandName = "나이키",
                    quantity = 2,
                    unitPrice = BigDecimal("129000"),
                ),
                OrderItemCommand(
                    productId = 2L,
                    productName = "울트라부스트",
                    brandName = "아디다스",
                    quantity = 1,
                    unitPrice = BigDecimal("199000"),
                ),
            )

            // act
            val order = orderDomainService.buildOrder(1L, items)

            // assert
            assertAll(
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.orderItems).hasSize(2) },
                { assertThat(order.totalAmount).isEqualByComparingTo(BigDecimal("457000")) },
            )
        }

        @DisplayName("아이템이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenItemsEmpty() {
            // act
            val exception = assertThrows<CoreException> {
                orderDomainService.buildOrder(1L, emptyList())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
