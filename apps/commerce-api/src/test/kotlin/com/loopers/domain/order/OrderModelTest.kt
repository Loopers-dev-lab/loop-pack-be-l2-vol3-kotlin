package com.loopers.domain.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderModel")
class OrderModelTest {

    companion object {
        private const val VALID_USER_ID = 1L
    }

    private fun createOrderItem(
        order: OrderModel,
        productId: Long = 1L,
        productName: String = "감성 티셔츠",
        brandName: String = "루프팩",
        price: Long = 25000L,
        quantity: Int = 2,
    ): OrderItemModel = OrderItemModel(
        order = order,
        productId = productId,
        productName = productName,
        brandName = brandName,
        price = price,
        quantity = quantity,
    )

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("주문 생성 시 초기 상태는 ORDERED이고 totalAmount는 0이다")
        @Test
        fun createsOrderModel_withInitialState() {
            // arrange & act
            val order = OrderModel(userId = VALID_USER_ID)

            // assert
            assertThat(order.userId).isEqualTo(VALID_USER_ID)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.ORDERED)
            assertThat(order.totalAmount).isEqualTo(0L)
            assertThat(order.orderItems).isEmpty()
        }
    }

    @DisplayName("addItem")
    @Nested
    inner class AddItem {
        @DisplayName("항목 1개를 추가하면 totalAmount가 해당 항목의 subTotal과 같다")
        @Test
        fun recalculatesTotalAmount_whenSingleItemAdded() {
            // arrange
            val order = OrderModel(userId = VALID_USER_ID)
            val item = createOrderItem(order, price = 25000L, quantity = 2)

            // act
            order.addItem(item)

            // assert
            assertThat(order.orderItems).hasSize(1)
            assertThat(order.totalAmount).isEqualTo(50000L)
        }

        @DisplayName("여러 항목을 추가하면 totalAmount가 모든 항목의 subTotal 합계와 같다")
        @Test
        fun recalculatesTotalAmount_whenMultipleItemsAdded() {
            // arrange
            val order = OrderModel(userId = VALID_USER_ID)
            val item1 = createOrderItem(
                order,
                productId = 1L,
                productName = "감성 티셔츠",
                price = 25000L,
                quantity = 2,
            )
            val item2 = createOrderItem(
                order,
                productId = 2L,
                productName = "캔버스백",
                price = 5000L,
                quantity = 1,
            )

            // act
            order.addItem(item1)
            order.addItem(item2)

            // assert
            assertThat(order.orderItems).hasSize(2)
            assertThat(order.totalAmount).isEqualTo(25000L * 2 + 5000L * 1)
        }

        @DisplayName("항목 추가 시 총 금액이 정확히 계산된다 (25000*2 + 5000*1 = 55000)")
        @Test
        fun calculatesExactTotalAmount() {
            // arrange
            val order = OrderModel(userId = VALID_USER_ID)

            // act
            order.addItem(createOrderItem(order, productId = 1L, price = 25000L, quantity = 2))
            order.addItem(createOrderItem(order, productId = 2L, price = 5000L, quantity = 1))

            // assert
            assertThat(order.totalAmount).isEqualTo(55000L)
        }
    }
}
