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
                { assertThat(order.originalTotalPrice).isEqualTo(199000L * 2 + 179000L * 1) },
                { assertThat(order.discountAmount).isEqualTo(0L) },
                { assertThat(order.couponId).isNull() },
            )
        }

        @DisplayName("쿠폰 할인이 적용되면, totalPrice에서 할인 금액이 차감된다.")
        @Test
        fun appliesDiscountAmount_whenCouponIsUsed() {
            // arrange
            val items = listOf(
                OrderItem(productId = 1L, productName = "에어맥스", productPrice = 100000L, quantity = 1),
            )

            // act
            val order = Order(userId = 1L, items = items, couponId = 1L, discountAmount = 3000L)

            // assert
            assertAll(
                { assertThat(order.originalTotalPrice).isEqualTo(100000L) },
                { assertThat(order.discountAmount).isEqualTo(3000L) },
                { assertThat(order.totalPrice).isEqualTo(97000L) },
                { assertThat(order.couponId).isEqualTo(1L) },
            )
        }

        @DisplayName("할인 금액이 총 금액을 초과하면, totalPrice는 0이 된다.")
        @Test
        fun totalPriceIsZero_whenDiscountExceedsOriginalPrice() {
            // arrange
            val items = listOf(
                OrderItem(productId = 1L, productName = "에어맥스", productPrice = 2000L, quantity = 1),
            )

            // act
            val order = Order(userId = 1L, items = items, couponId = 1L, discountAmount = 5000L)

            // assert
            assertAll(
                { assertThat(order.originalTotalPrice).isEqualTo(2000L) },
                { assertThat(order.discountAmount).isEqualTo(5000L) },
                { assertThat(order.totalPrice).isEqualTo(0L) },
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
