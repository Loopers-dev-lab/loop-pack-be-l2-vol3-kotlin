package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.ZoneId

class OrderTest {

    private fun createOrder(userId: Long = 1L): Order = Order(userId = userId)

    private fun createOrderItem(
        productId: Long = 1L,
        quantity: Int = 2,
        finalPrice: Money = Money(10000),
    ): OrderItem = OrderItem(
        productId = productId,
        quantity = quantity,
        productSnapshot = ProductSnapshot(
            productName = "상품",
            brandName = "브랜드",
            brandId = 1L,
            imageUrl = null,
        ),
        priceSnapshot = PriceSnapshot(
            originalPrice = finalPrice,
            discountAmount = Money.ZERO,
            finalPrice = finalPrice,
        ),
    )

    @Nested
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 주문을 생성하면 성공한다")
        fun success() {
            // arrange & act
            val order = createOrder()

            // assert
            assertThat(order.userId).isEqualTo(1L)
            assertThat(order.orderStatus).isEqualTo(OrderStatus.ORDERED)
            assertThat(order.totalAmount).isEqualTo(Money.ZERO)
            assertThat(order.orderItems).isEmpty()
        }

        @Test
        @DisplayName("userId가 0이면 BAD_REQUEST 예외가 발생한다")
        fun zeroUserIdThrowsBadRequest() {
            // act
            val result = assertThrows<CoreException> {
                createOrder(userId = 0)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("userId가 음수이면 BAD_REQUEST 예외가 발생한다")
        fun negativeUserIdThrowsBadRequest() {
            // act
            val result = assertThrows<CoreException> {
                createOrder(userId = -1)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class AddItem {

        @Test
        @DisplayName("주문 항목을 추가하면 orderItems에 포함된다")
        fun success() {
            // arrange
            val order = createOrder()
            val item = createOrderItem()

            // act
            order.addItem(item)

            // assert
            assertThat(order.orderItems).hasSize(1)
            assertThat(item.order).isEqualTo(order)
        }
    }

    @Nested
    inner class CalculateTotalAmount {

        @Test
        @DisplayName("주문 항목들의 총액을 합산한다")
        fun success() {
            // arrange
            val order = createOrder()
            order.addItem(createOrderItem(productId = 1L, quantity = 2, finalPrice = Money(10000))) // 20000
            order.addItem(createOrderItem(productId = 2L, quantity = 3, finalPrice = Money(5000))) // 15000

            // act
            order.calculateTotalAmount()

            // assert
            assertThat(order.totalAmount).isEqualTo(Money(35000))
        }
    }

    @Nested
    inner class GenerateOrderNumber {

        @Test
        @DisplayName("주문번호가 yyMMdd + 8자리 id 형식으로 생성된다")
        fun success() {
            // arrange
            val order = createOrder()
            setEntityId(order, 42L)

            // act
            order.generateOrderNumber()

            // assert
            val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val expectedDate = String.format("%02d%02d%02d", today.year % 100, today.monthValue, today.dayOfMonth)
            assertThat(order.orderNumber).isEqualTo("${expectedDate}00000042")
            assertThat(order.orderNumber).hasSize(14)
        }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        val idField = BaseEntity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }
}
