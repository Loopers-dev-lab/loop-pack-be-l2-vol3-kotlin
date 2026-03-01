package com.loopers.domain.coupon

import com.loopers.domain.common.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DiscountTest {

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    inner class CalculateDiscountAmount {

        @DisplayName("정액 할인이면, 할인값만큼 할인된다.")
        @Test
        fun returnsFixedAmount_whenFixedAmountType() {
            // arrange
            val discount = Discount(DiscountType.FIXED_AMOUNT, 5000L)
            val totalAmount = Money.of(100000L)

            // act
            val discountAmount = discount.calculateDiscountAmount(totalAmount)

            // assert
            assertThat(discountAmount).isEqualTo(Money.of(5000L))
        }

        @DisplayName("정액 할인이 주문 금액보다 크면, 주문 금액만큼만 할인된다.")
        @Test
        fun capsAtTotalAmount_whenFixedAmountExceedsTotal() {
            // arrange
            val discount = Discount(DiscountType.FIXED_AMOUNT, 50000L)
            val totalAmount = Money.of(30000L)

            // act
            val discountAmount = discount.calculateDiscountAmount(totalAmount)

            // assert
            assertThat(discountAmount).isEqualTo(Money.of(30000L))
        }

        @DisplayName("정률 할인이면, 비율만큼 할인된다.")
        @Test
        fun returnsPercentageAmount_whenPercentageType() {
            // arrange
            val discount = Discount(DiscountType.PERCENTAGE, 10L)
            val totalAmount = Money.of(100000L)

            // act
            val discountAmount = discount.calculateDiscountAmount(totalAmount)

            // assert
            assertThat(discountAmount).isEqualTo(Money.of(10000L))
        }

        @DisplayName("정률 할인 시 소수점 이하는 내림 처리된다.")
        @Test
        fun floorsDecimal_whenPercentageType() {
            // arrange
            val discount = Discount(DiscountType.PERCENTAGE, 15L)
            val totalAmount = Money.of(33333L)

            // act
            val discountAmount = discount.calculateDiscountAmount(totalAmount)

            // assert
            // 33333 * 15 / 100 = 4999 (Long 나눗셈으로 자연스럽게 내림)
            assertThat(discountAmount).isEqualTo(Money.of(4999L))
        }
    }
}
