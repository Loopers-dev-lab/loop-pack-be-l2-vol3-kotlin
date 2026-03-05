package com.loopers.domain.order

import com.loopers.domain.common.vo.CouponId
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

        @Test
        @DisplayName("쿠폰 미적용 시 originalPrice와 totalPrice가 동일하고 discountAmount는 0이다")
        fun create_withoutCoupon_originalEqualsTotalPrice() {
            // act
            val order = Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
                ),
            )

            // assert
            assertThat(order.originalPrice.value).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(order.discountAmount.value).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(order.refCouponId).isNull()
        }

        @Test
        @DisplayName("쿠폰 적용 시 originalPrice, discountAmount, totalPrice 정합성이 유지된다")
        fun create_withCoupon_discountApplied() {
            // act
            val order = Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
                ),
                discountAmount = Money(BigDecimal("3000")),
                refCouponId = CouponId(100L),
            )

            // assert
            assertThat(order.originalPrice.value).isEqualByComparingTo(BigDecimal("20000"))
            assertThat(order.discountAmount.value).isEqualByComparingTo(BigDecimal("3000"))
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("17000"))
            assertThat(order.refCouponId).isEqualTo(CouponId(100L))
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
        @DisplayName("할인 적용된 주문에서 아이템 취소 시 totalPrice가 음수가 되지 않고 재계산된다")
        fun cancelItem_discountedOrder_totalPriceNotNegative() {
            // arrange
            // originalPrice = 10000*1 = 10000, discountAmount = 3000, totalPrice = 7000
            val order = Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(1),
                ),
                discountAmount = Money(BigDecimal("3000")),
                refCouponId = CouponId(100L),
            )
            val item = order.items[0]

            // act
            order.cancelItem(item)

            // assert
            // 활성 아이템 합계 = 0, 적용할 할인 = min(3000, 0) = 0, totalPrice = 0
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        @DisplayName("할인 적용된 주문에서 일부 아이템 취소 시 활성 아이템 기준으로 totalPrice가 재계산된다")
        fun cancelItem_discountedOrder_partialCancel_totalPriceRecalculated() {
            // arrange
            // originalPrice = 10000*1 + 20000*1 = 30000, discountAmount = 5000, totalPrice = 25000
            val order = Order.create(
                UserId(1),
                listOf(
                    OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(1),
                    OrderProductData(id = ProductId(2), name = "상품B", price = Money(BigDecimal("20000"))) to Quantity(1),
                ),
                discountAmount = Money(BigDecimal("5000")),
                refCouponId = CouponId(100L),
            )
            val item = order.items[0] // 상품A: 10000

            // act
            order.cancelItem(item)

            // assert
            // 활성 아이템 합계 = 20000, 적용할 할인 = min(5000, 20000) = 5000, totalPrice = 15000
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("15000"))
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

    @Nested
    @DisplayName("할인 금액 불변식")
    inner class DiscountAmountInvariant {

        @Test
        @DisplayName("음수 할인 금액으로 생성하면 예외가 발생한다")
        fun create_negativeDiscount_throwsException() {
            // arrange & act & assert
            assertThrows<CoreException> {
                Money(BigDecimal.valueOf(-1000))
            }
        }

        @Test
        @DisplayName("할인 금액이 원가를 초과하면 BAD_REQUEST 예외가 발생한다")
        fun create_excessiveDiscount_throwsException() {
            // arrange
            val items = listOf(
                OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
            )
            // items의 원가: 10000 * 2 = 20000
            val excessiveDiscount = Money(BigDecimal("20001"))

            // act
            val exception = assertThrows<CoreException> {
                Order.create(UserId(1L), items, excessiveDiscount, CouponId(1L))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("할인 금액이 있으면서 쿠폰 참조가 없으면 BAD_REQUEST 예외가 발생한다")
        fun create_discountWithoutCoupon_throwsBadRequest() {
            // arrange
            val items = listOf(
                OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
            )
            val discount = Money(BigDecimal("1000"))

            // act
            val exception = assertThrows<CoreException> {
                Order.create(UserId(1L), items, discount, null)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("정상 할인 금액으로 주문을 생성한다")
        fun create_validDiscount_success() {
            // arrange
            val items = listOf(
                OrderProductData(id = ProductId(1), name = "상품A", price = Money(BigDecimal("10000"))) to Quantity(2),
            )
            val validDiscount = Money(BigDecimal("1000"))

            // act
            val order = Order.create(UserId(1L), items, validDiscount, CouponId(1L))

            // assert
            assertThat(order.discountAmount).isEqualTo(validDiscount)
            assertThat(order.totalPrice.value).isEqualByComparingTo(BigDecimal("19000"))
        }
    }
}
