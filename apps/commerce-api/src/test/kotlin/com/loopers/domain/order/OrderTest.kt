package com.loopers.domain.order

import com.loopers.domain.product.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderTest {

    private fun createSnapshot(
        productId: Long = 1L,
        productName: String = "테스트 상품",
        productPrice: Money = Money(10000),
        brandName: String = "나이키",
        imageUrl: String = "https://example.com/image.jpg",
        quantity: Quantity = Quantity(2),
    ): OrderItemSnapshot {
        return OrderItemSnapshot(
            productId = productId,
            productName = productName,
            productPrice = productPrice,
            brandName = brandName,
            imageUrl = imageUrl,
            quantity = quantity,
        )
    }

    @DisplayName("주문 생성")
    @Nested
    inner class Create {

        @DisplayName("스냅샷 목록으로 생성하면 총액이 정확히 계산된다")
        @Test
        fun successWithCorrectTotalAmount() {
            val items = listOf(
                createSnapshot(productId = 1L, productPrice = Money(10000), quantity = Quantity(2)),
                createSnapshot(productId = 2L, productPrice = Money(5000), quantity = Quantity(3)),
            )

            val order = Order.create(userId = 1L, items = items)

            assertAll(
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.totalAmount).isEqualTo(Money(35000)) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
            )
        }

        @DisplayName("단일 상품 주문 시 총액이 가격 × 수량이다")
        @Test
        fun successSingleItem() {
            val items = listOf(
                createSnapshot(productPrice = Money(15000), quantity = Quantity(1)),
            )

            val order = Order.create(userId = 1L, items = items)

            assertThat(order.totalAmount).isEqualTo(Money(15000))
        }

        @DisplayName("빈 스냅샷 목록이면 EMPTY_ORDER_ITEMS 에러가 발생한다")
        @Test
        fun failWhenEmptyItems() {
            val exception = assertThrows<CoreException> {
                Order.create(userId = 1L, items = emptyList())
            }
            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.EMPTY_ORDER_ITEMS)
        }
    }
}
