package com.loopers.domain.order

import com.loopers.domain.order.entity.Order
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class OrderTest {

    private fun createProductInfo(id: Long, name: String, price: BigDecimal): OrderProductInfo {
        return OrderProductInfo(id = id, name = name, price = price)
    }

    @Nested
    @DisplayName("Order.create 시")
    inner class Create {

        @Test
        @DisplayName("정상적인 주문이 생성된다")
        fun create_validOrder_success() {
            // arrange
            val product = createProductInfo(1L, "에어맥스 90", BigDecimal("129000"))
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = 2)),
            )

            // act
            val order = Order.create(1L, listOf(product), command)

            // assert
            assertThat(order.refUserId).isEqualTo(1L)
            assertThat(order.status).isEqualTo(Order.OrderStatus.CREATED)
            assertThat(order.items).hasSize(1)
            assertThat(order.totalPrice).isEqualByComparingTo(BigDecimal("258000"))
        }

        @Test
        @DisplayName("여러 상품이 포함된 주문의 총액이 올바르게 계산된다")
        fun create_multipleItems_calculatesTotalPrice() {
            // arrange
            val product1 = createProductInfo(1L, "상품1", BigDecimal("10000"))
            val product2 = createProductInfo(2L, "상품2", BigDecimal("20000"))
            val command = OrderCommand.CreateOrder(
                items = listOf(
                    OrderCommand.CreateOrderItem(productId = 1L, quantity = 3),
                    OrderCommand.CreateOrderItem(productId = 2L, quantity = 2),
                ),
            )

            // act
            val order = Order.create(1L, listOf(product1, product2), command)

            // assert
            assertThat(order.items).hasSize(2)
            assertThat(order.totalPrice).isEqualByComparingTo(BigDecimal("70000"))
        }

        @Test
        @DisplayName("주문 항목에 상품 스냅샷이 저장된다")
        fun create_snapshotsProductInfo() {
            // arrange
            val product = createProductInfo(1L, "에어맥스 90", BigDecimal("129000"))
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = 1L, quantity = 1)),
            )

            // act
            val order = Order.create(1L, listOf(product), command)

            // assert
            val item = order.items[0]
            assertThat(item.refProductId).isEqualTo(1L)
            assertThat(item.productName).isEqualTo("에어맥스 90")
            assertThat(item.productPrice).isEqualByComparingTo(BigDecimal("129000"))
            assertThat(item.quantity).isEqualTo(1)
        }

        @Test
        @DisplayName("주문 항목이 비어있으면 BAD_REQUEST 예외가 발생한다")
        fun create_emptyItems_throwsException() {
            // arrange
            val command = OrderCommand.CreateOrder(items = emptyList())

            // act
            val exception = assertThrows<CoreException> {
                Order.create(1L, emptyList(), command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("주문 항목이 비어있습니다")
        }

        @Test
        @DisplayName("중복된 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun create_duplicateProducts_throwsException() {
            // arrange
            val product = createProductInfo(1L, "상품1", BigDecimal("10000"))
            val command = OrderCommand.CreateOrder(
                items = listOf(
                    OrderCommand.CreateOrderItem(productId = 1L, quantity = 1),
                    OrderCommand.CreateOrderItem(productId = 1L, quantity = 2),
                ),
            )

            // act
            val exception = assertThrows<CoreException> {
                Order.create(1L, listOf(product), command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("중복된 상품")
        }

        @Test
        @DisplayName("존재하지 않는 상품이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun create_missingProduct_throwsException() {
            // arrange
            val command = OrderCommand.CreateOrder(
                items = listOf(OrderCommand.CreateOrderItem(productId = 999L, quantity = 1)),
            )

            // act
            val exception = assertThrows<CoreException> {
                Order.create(1L, emptyList(), command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).contains("상품을 찾을 수 없습니다")
        }
    }
}
