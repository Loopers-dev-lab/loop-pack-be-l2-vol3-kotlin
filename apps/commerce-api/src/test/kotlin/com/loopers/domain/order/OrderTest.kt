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

class OrderTest {

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("정상적인 userId가 주어지면, 주문이 생성된다.")
        @Test
        fun createsOrder_whenValidUserIdProvided() {
            // arrange
            val userId = 1L

            // act
            val order = Order(userId = userId)

            // assert
            assertAll(
                { assertThat(order.userId).isEqualTo(userId) },
                { assertThat(order.totalAmount).isEqualByComparingTo(BigDecimal.ZERO) },
                { assertThat(order.orderItems).isEmpty() },
            )
        }

        @DisplayName("userId가 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenUserIdIsZeroOrLess() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                Order(userId = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문 항목을 추가할 때,")
    @Nested
    inner class AddItem {

        @DisplayName("정상적인 항목이 주어지면, 항목이 추가되고 총 금액이 업데이트된다.")
        @Test
        fun addsItemAndUpdatesTotalAmount_whenValidItemProvided() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.addItem(
                productId = 1L,
                productName = "에어맥스 90",
                brandName = "나이키",
                quantity = 2,
                unitPrice = BigDecimal("129000"),
            )

            // assert
            assertAll(
                { assertThat(order.orderItems).hasSize(1) },
                { assertThat(order.totalAmount).isEqualByComparingTo(BigDecimal("258000")) },
                { assertThat(order.orderItems[0].productId).isEqualTo(1L) },
                { assertThat(order.orderItems[0].productName).isEqualTo("에어맥스 90") },
                { assertThat(order.orderItems[0].brandName).isEqualTo("나이키") },
                { assertThat(order.orderItems[0].quantity).isEqualTo(2) },
                { assertThat(order.orderItems[0].unitPrice).isEqualByComparingTo(BigDecimal("129000")) },
            )
        }

        @DisplayName("수량이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenQuantityIsZeroOrLess() {
            // arrange
            val order = Order(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                order.addItem(
                    productId = 1L,
                    productName = "에어맥스 90",
                    brandName = "나이키",
                    quantity = 0,
                    unitPrice = BigDecimal("129000"),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("단가가 음수이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenUnitPriceIsNegative() {
            // arrange
            val order = Order(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                order.addItem(
                    productId = 1L,
                    productName = "에어맥스 90",
                    brandName = "나이키",
                    quantity = 1,
                    unitPrice = BigDecimal("-1"),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("총 금액을 계산할 때,")
    @Nested
    inner class CalculateTotalAmount {

        @DisplayName("여러 항목이 있으면, 정확한 총액이 계산된다.")
        @Test
        fun calculatesCorrectTotal_whenMultipleItemsExist() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.addItem(productId = 1L, productName = "에어맥스 90", brandName = "나이키", quantity = 2, unitPrice = BigDecimal("129000"))
            order.addItem(productId = 2L, productName = "울트라부스트", brandName = "아디다스", quantity = 1, unitPrice = BigDecimal("199000"))

            // assert
            assertThat(order.totalAmount).isEqualByComparingTo(BigDecimal("457000"))
        }
    }

    @DisplayName("주문 항목이 비어있는지 검증할 때,")
    @Nested
    inner class ValidateNotEmpty {

        @DisplayName("주문 항목이 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNoItemsExist() {
            // arrange
            val order = Order(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                order.validateNotEmpty()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
