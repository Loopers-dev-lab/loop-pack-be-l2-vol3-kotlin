package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderTest {

    @DisplayName("주문 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsOrder_whenValidValuesProvided() {
            // arrange & act
            val order = Order(userId = 1L)

            // assert
            assertAll(
                { assertThat(order.userId).isEqualTo(1L) },
                { assertThat(order.totalAmount).isEqualTo(Money.ZERO) },
                { assertThat(order.status).isEqualTo(OrderStatus.ORDERED) },
                { assertThat(order.items).isEmpty() },
            )
        }
    }

    @DisplayName("주문 항목을 추가할 때,")
    @Nested
    inner class AddItems {

        @DisplayName("항목이 추가되고 총 금액이 계산된다.")
        @Test
        fun addsItemAndCalculatesTotalAmount() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.addItems(
                listOf(
                    OrderItemCommand(
                        productId = 1L,
                        quantity = Quantity.of(2),
                        productName = "에어맥스",
                        productPrice = Money.of(159000L),
                        brandName = "나이키",
                    ),
                ),
            )

            // assert
            assertAll(
                { assertThat(order.items).hasSize(1) },
                { assertThat(order.totalAmount).isEqualTo(Money.of(318000L)) },
            )
        }

        @DisplayName("여러 항목을 추가하면 총 금액이 합산된다.")
        @Test
        fun calculatesTotalAmountForMultipleItems() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.addItems(
                listOf(
                    OrderItemCommand(
                        productId = 1L,
                        quantity = Quantity.of(2),
                        productName = "에어맥스",
                        productPrice = Money.of(159000L),
                        brandName = "나이키",
                    ),
                    OrderItemCommand(
                        productId = 2L,
                        quantity = Quantity.of(3),
                        productName = "에어포스",
                        productPrice = Money.of(139000L),
                        brandName = "나이키",
                    ),
                ),
            )

            // assert
            assertAll(
                { assertThat(order.items).hasSize(2) },
                // 159000 * 2 + 139000 * 3 = 318000 + 417000 = 735000
                { assertThat(order.totalAmount).isEqualTo(Money.of(735000L)) },
            )
        }

        @DisplayName("주문 항목이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenItemsEmpty() {
            // arrange
            val order = Order(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                order.addItems(emptyList())
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("중복된 상품이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenDuplicateProductIds() {
            // arrange
            val order = Order(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                order.addItems(
                    listOf(
                        OrderItemCommand(
                            productId = 1L,
                            quantity = Quantity.of(1),
                            productName = "에어맥스",
                            productPrice = Money.of(159000L),
                            brandName = "나이키",
                        ),
                        OrderItemCommand(
                            productId = 1L,
                            quantity = Quantity.of(2),
                            productName = "에어맥스",
                            productPrice = Money.of(159000L),
                            brandName = "나이키",
                        ),
                    ),
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 0 이하이면, Quantity 생성 시 BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZeroOrNegative() {
            // act
            val exception = assertThrows<CoreException> {
                Quantity.of(0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("쿠폰 할인을 적용할 때,")
    @Nested
    inner class ApplyCouponDiscount {

        @DisplayName("할인 금액과 결제 금액이 정상적으로 설정된다.")
        @Test
        fun setsDiscountAndPaymentAmount() {
            // arrange
            val order = Order(userId = 1L)
            order.addItems(
                listOf(
                    OrderItemCommand(
                        productId = 1L,
                        quantity = Quantity.of(2),
                        productName = "에어맥스",
                        productPrice = Money.of(100000L),
                        brandName = "나이키",
                    ),
                ),
            )

            // act
            order.applyCouponDiscount(couponId = 42L, discountAmount = Money.of(10000L))

            // assert
            assertAll(
                { assertThat(order.couponId).isEqualTo(42L) },
                { assertThat(order.discountAmount).isEqualTo(Money.of(10000L)) },
                { assertThat(order.paymentAmount).isEqualTo(Money.of(190000L)) },
            )
        }

        @DisplayName("쿠폰 미적용 시, 할인 금액은 0이고 결제 금액은 총 금액과 같다.")
        @Test
        fun noDiscount_whenCouponNotApplied() {
            // arrange
            val order = Order(userId = 1L)
            order.addItems(
                listOf(
                    OrderItemCommand(
                        productId = 1L,
                        quantity = Quantity.of(1),
                        productName = "에어맥스",
                        productPrice = Money.of(100000L),
                        brandName = "나이키",
                    ),
                ),
            )

            // assert
            assertAll(
                { assertThat(order.couponId).isNull() },
                { assertThat(order.discountAmount).isEqualTo(Money.ZERO) },
                { assertThat(order.paymentAmount).isEqualTo(Money.of(100000L)) },
            )
        }
    }

    @DisplayName("주문 상태를 변경할 때,")
    @Nested
    inner class ChangeStatus {

        @DisplayName("유효한 전이이면, 상태가 변경된다.")
        @Test
        fun changesStatus_whenValidTransition() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.changeStatus(OrderStatus.CONFIRMED)

            // assert
            assertThat(order.status).isEqualTo(OrderStatus.CONFIRMED)
        }

        @DisplayName("ORDERED에서 CANCELLED로 전이할 수 있다.")
        @Test
        fun transitionsFromOrderedToCancelled() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.changeStatus(OrderStatus.CANCELLED)

            // assert
            assertThat(order.status).isEqualTo(OrderStatus.CANCELLED)
        }

        @DisplayName("연속 전이가 가능하다 (ORDERED → CONFIRMED → SHIPPING → DELIVERED).")
        @Test
        fun transitionsThroughFullLifecycle() {
            // arrange
            val order = Order(userId = 1L)

            // act
            order.changeStatus(OrderStatus.CONFIRMED)
            order.changeStatus(OrderStatus.SHIPPING)
            order.changeStatus(OrderStatus.DELIVERED)

            // assert
            assertThat(order.status).isEqualTo(OrderStatus.DELIVERED)
        }

        @DisplayName("허용되지 않은 전이이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenInvalidTransition() {
            // arrange
            val order = Order(userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                order.changeStatus(OrderStatus.DELIVERED)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("종료 상태에서는 전이할 수 없다.")
        @Test
        fun throwsBadRequest_whenTransitionFromTerminalState() {
            // arrange
            val order = Order(userId = 1L)
            order.changeStatus(OrderStatus.CANCELLED)

            // act
            val exception = assertThrows<CoreException> {
                order.changeStatus(OrderStatus.ORDERED)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
