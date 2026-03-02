package com.loopers.domain.order

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.common.vo.Quantity
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
        @DisplayName("items 기반으로 주문이 생성되고 totalPrice가 자동 계산된다")
        fun create_withItems_computesTotalPrice() {
            // act
            val order = Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
                    OrderProductData(id = ProductId(2), name = "상품B", price = Money(BigDecimal("20000"))) to Quantity(1),
                ),
            )

            // assert
            assertThat(order.refUserId).isEqualTo(UserId(1))
            assertThat(order.status).isEqualTo(Order.OrderStatus.CREATED)
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("40000"))
            assertThat(order.items).hasSize(2)
        }

        @Test
        @DisplayName("단일 상품 주문이 정상 생성된다")
        fun create_singleItem_success() {
            // act
            val order = Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("129000"))) to Quantity(2),
                ),
            )

            // assert
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("258000"))
            assertThat(order.items).hasSize(1)
        }
    }

    @Nested
    @DisplayName("cancelItem 시")
    inner class CancelItem {

        private fun createOrderWithItems(): Order =
            Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
                    OrderProductData(id = ProductId(2), name = "상품B", price = Money(BigDecimal("10000"))) to Quantity(1),
                ),
            )

        @Test
        @DisplayName("아이템을 취소하면 해당 아이템의 status가 CANCELLED로 변경된다")
        fun cancelItem_validItem_itemStatusCancelled() {
            // arrange
            val order = createOrderWithItems()
            val item = order.items[0]

            // act
            order.cancelItem(item)

            // assert
            assertThat(item.status).isEqualTo(OrderItem.ItemStatus.CANCELLED)
        }

        @Test
        @DisplayName("아이템을 취소하면 totalPrice가 해당 아이템 금액만큼 차감된다")
        fun cancelItem_validItem_totalPriceReduced() {
            // arrange
            val order = createOrderWithItems() // totalPrice = 10000*2 + 10000*1 = 30000
            val item = order.items[0] // 10000 * 2 = 20000

            // act
            order.cancelItem(item)

            // assert
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("10000")) // 30000 - 20000
        }

        @Test
        @DisplayName("이미 취소된 아이템을 다시 취소하면 BAD_REQUEST 예외가 발생한다")
        fun cancelItem_alreadyCancelled_throwsException() {
            // arrange
            val order = createOrderWithItems()
            val item = order.items[0]
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
