package com.loopers.domain.order

import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderItemTest {

    private fun createOrderItem(
        productId: Long = 1L,
        quantity: Int = 2,
        productSnapshot: ProductSnapshot = ProductSnapshot(
            productName = "에어맥스 90",
            brandName = "나이키",
            brandId = 1L,
            imageUrl = "https://example.com/airmax90.png",
        ),
        priceSnapshot: PriceSnapshot = PriceSnapshot(
            originalPrice = Money(139000),
            discountAmount = Money.ZERO,
            finalPrice = Money(139000),
        ),
    ): OrderItem = OrderItem(
        productId = productId,
        quantity = quantity,
        productSnapshot = productSnapshot,
        priceSnapshot = priceSnapshot,
    )

    @Nested
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 주문 항목을 생성하면 성공한다")
        fun success() {
            // arrange & act
            val orderItem = createOrderItem()

            // assert
            assertThat(orderItem.productId).isEqualTo(1L)
            assertThat(orderItem.quantity).isEqualTo(2)
            assertThat(orderItem.itemStatus).isEqualTo(OrderItemStatus.ORDERED)
            assertThat(orderItem.productSnapshot.productName).isEqualTo("에어맥스 90")
            assertThat(orderItem.priceSnapshot.finalPrice).isEqualTo(Money(139000))
        }

        @Test
        @DisplayName("수량이 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroQuantityThrowsBadRequest() {
            // act
            val result = assertThrows<CoreException> {
                createOrderItem(quantity = 0)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("수량이 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeQuantityThrowsBadRequest() {
            // act
            val result = assertThrows<CoreException> {
                createOrderItem(quantity = -1)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class ItemTotalPrice {

        @Test
        @DisplayName("상품 단가 × 수량으로 항목 총액을 계산한다")
        fun success() {
            // arrange
            val orderItem = createOrderItem(quantity = 3)

            // act
            val totalPrice = orderItem.itemTotalPrice()

            // assert
            assertThat(totalPrice).isEqualTo(Money(417000)) // 139000 * 3
        }
    }
}
