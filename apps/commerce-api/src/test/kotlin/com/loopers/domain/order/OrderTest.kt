package com.loopers.domain.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class OrderTest {
    @DisplayName("주문을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("주문 항목이 주어지면, totalPrice가 자동 계산된다.")
        @Test
        fun calculatesTotalPrice_whenOrderItemsAreProvided() {
            // arrange
            val items = listOf(
                OrderItem(productId = 1L, productName = "에어맥스", productPrice = 199000L, quantity = 2),
                OrderItem(productId = 2L, productName = "울트라부스트", productPrice = 179000L, quantity = 1),
            )

            // act
            val order = Order(userId = 1L, items = items)

            // assert
            assertAll(
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.items).hasSize(2) },
                { assertThat(order.totalPrice).isEqualTo(199000L * 2 + 179000L * 1) },
            )
        }
    }

    @DisplayName("주문 항목을 생성할 때, ")
    @Nested
    inner class CreateOrderItem {
        @DisplayName("상품 스냅샷 정보가 저장된다.")
        @Test
        fun storesProductSnapshot() {
            // arrange & act
            val item = OrderItem(productId = 1L, productName = "에어맥스", productPrice = 199000L, quantity = 3)

            // assert
            assertAll(
                { assertThat(item.productId).isEqualTo(1L) },
                { assertThat(item.productName).isEqualTo("에어맥스") },
                { assertThat(item.productPrice).isEqualTo(199000L) },
                { assertThat(item.quantity).isEqualTo(3) },
            )
        }
    }
}
