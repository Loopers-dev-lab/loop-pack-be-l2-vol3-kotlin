package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.order.entity.Order
import com.loopers.domain.order.entity.OrderItem
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class OrderTest {

    @Nested
    @DisplayName("Order.create 시")
    inner class Create {

        @Test
        @DisplayName("정상적인 주문이 생성된다")
        fun create_validOrder_success() {
            // act
            val order = Order.create(1L, Money(BigDecimal("258000")))

            // assert
            assertThat(order.refUserId).isEqualTo(1L)
            assertThat(order.status).isEqualTo(Order.OrderStatus.CREATED)
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("258000"))
        }
    }

    @Nested
    @DisplayName("cancelItem 시")
    inner class CancelItem {

        private fun createItem(price: BigDecimal, quantity: Int): OrderItem =
            OrderItem.create(
                product = OrderProductInfo(id = 1L, name = "상품A", price = Money(price)),
                quantity = quantity,
                orderId = 0L,
            )

        @Test
        @DisplayName("아이템을 취소하면 해당 아이템의 status가 CANCELLED로 변경된다")
        fun cancelItem_validItem_itemStatusCancelled() {
            // arrange
            val order = Order.create(1L, Money(BigDecimal("20000")))
            val item = createItem(price = BigDecimal("10000"), quantity = 2)

            // act
            order.cancelItem(item)

            // assert
            assertThat(item.status).isEqualTo(OrderItem.ItemStatus.CANCELLED)
        }

        @Test
        @DisplayName("아이템을 취소하면 totalPrice가 해당 아이템 금액만큼 차감된다")
        fun cancelItem_validItem_totalPriceReduced() {
            // arrange
            val order = Order.create(1L, Money(BigDecimal("30000")))
            val item = createItem(price = BigDecimal("10000"), quantity = 2)

            // act
            order.cancelItem(item)

            // assert
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("10000")) // 30000 - 10000*2
        }

        @Test
        @DisplayName("이미 취소된 아이템을 다시 취소하면 BAD_REQUEST 예외가 발생한다")
        fun cancelItem_alreadyCancelled_throwsException() {
            // arrange
            val order = Order.create(1L, Money(BigDecimal("20000")))
            val item = createItem(price = BigDecimal("10000"), quantity = 2)
            order.cancelItem(item)

            // act
            val exception = assertThrows<CoreException> {
                order.cancelItem(item)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
