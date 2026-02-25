package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class OrderTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_정보로_주문을_생성할_수_있다`() {
            // act
            val order = createOrder()

            // assert
            assertAll(
                { assertThat(order.memberId).isEqualTo(1L) },
                { assertThat(order.orderItems).hasSize(1) },
                { assertThat(order.totalPrice).isEqualTo(20000L) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @Test
        fun `주문항목이_비어있으면_예외가_발생한다`() {
            val result = assertThrows<CoreException> {
                Order(
                    memberId = 1L,
                    orderItems = emptyList(),
                    totalPrice = 0L,
                    orderedAt = ZonedDateTime.now(),
                )
            }
            assertThat(result.errorType).isEqualTo(ErrorType.ORDER_ITEM_EMPTY)
        }
    }

    @Nested
    inner class Cancel {
        @Test
        fun `주문을_취소할_수_있다`() {
            // arrange
            val order = createOrder()

            // act
            order.cancel()

            // assert
            assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        }

        @Test
        fun `이미_취소된_주문을_취소하면_예외가_발생한다`() {
            // arrange
            val order = createOrder()
            order.cancel()

            // act
            val result = assertThrows<CoreException> { order.cancel() }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.ORDER_ALREADY_CANCELLED)
        }
    }

    @Nested
    inner class ValidateOwner {
        @Test
        fun `소유자가_일치하면_예외가_발생하지_않는다`() {
            // arrange
            val order = createOrder(memberId = 1L)

            // act & assert (no exception)
            order.validateOwner(1L)
        }

        @Test
        fun `소유자가_일치하지_않으면_예외가_발생한다`() {
            // arrange
            val order = createOrder(memberId = 1L)

            // act
            val result = assertThrows<CoreException> { order.validateOwner(2L) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.ORDER_NOT_OWNER)
        }
    }

    private fun createOrder(memberId: Long = 1L): Order {
        val orderItem = OrderItem(
            productId = 1L,
            productName = "상품명",
            productPrice = 10000L,
            quantity = 2,
        )
        return Order(
            memberId = memberId,
            orderItems = listOf(orderItem),
            totalPrice = orderItem.subtotal,
            orderedAt = ZonedDateTime.now(),
        )
    }
}
