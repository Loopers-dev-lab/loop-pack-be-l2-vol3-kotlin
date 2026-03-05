package com.loopers.domain.order

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderModelTest {
    private fun createOrder(memberId: Long = 1L) = OrderModel(memberId = memberId)

    private fun createOrderItem(
        productId: Long = 1L,
        productName: String = "감성 티셔츠",
        productPrice: Long = 39000,
        brandName: String = "루퍼스",
        quantity: Int = 2,
    ) = OrderItemModel(
        productId = productId,
        productName = productName,
        productPrice = productPrice,
        brandName = brandName,
        quantity = quantity,
    )

    @DisplayName("주문 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("memberId가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrder_whenMemberIdIsProvided() {
            // act
            val order = createOrder()

            // assert
            assertAll(
                { assertThat(order.memberId).isEqualTo(1L) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(order.orderNumber).isNotBlank() },
                { assertThat(order.items).isEmpty() },
            )
        }
    }

    @DisplayName("주문 아이템을 추가할 때,")
    @Nested
    inner class AddItem {
        @DisplayName("아이템을 추가하면, 주문에 포함된다.")
        @Test
        fun addsItemToOrder() {
            // arrange
            val order = createOrder()
            val item = createOrderItem()

            // act
            val updated = order.addItem(item)

            // assert
            assertAll(
                { assertThat(updated.items).hasSize(1) },
                { assertThat(updated.items[0].productName).isEqualTo("감성 티셔츠") },
            )
        }
    }

    @DisplayName("총 주문 금액을 계산할 때,")
    @Nested
    inner class GetTotalAmount {
        @DisplayName("아이템이 있으면, 각 아이템의 amount 합계를 반환한다.")
        @Test
        fun returnsSumOfAmounts() {
            // arrange
            val order = createOrder()
                .addItem(createOrderItem(productPrice = 39000, quantity = 2))
                .addItem(createOrderItem(productId = 2L, productPrice = 15000, quantity = 1))

            // act
            val totalAmount = order.getTotalAmount()

            // assert
            assertThat(totalAmount).isEqualTo(39000 * 2 + 15000 * 1)
        }

        @DisplayName("아이템이 없으면, 0을 반환한다.")
        @Test
        fun returnsZero_whenNoItems() {
            val order = createOrder()
            assertThat(order.getTotalAmount()).isEqualTo(0)
        }
    }

    @DisplayName("주문 소유자를 검증할 때,")
    @Nested
    inner class ValidateOwner {
        @DisplayName("본인이면, 예외가 발생하지 않는다.")
        @Test
        fun doesNotThrow_whenOwner() {
            val order = createOrder(memberId = 1L)
            order.validateOwner(1L) // no exception
        }

        @DisplayName("본인이 아니면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsForbidden_whenNotOwner() {
            val order = createOrder(memberId = 1L)
            val result = assertThrows<CoreException> { order.validateOwner(2L) }
            assertThat(result.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
