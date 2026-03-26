package com.loopers.domain.payment

import com.loopers.domain.order.Order
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("DisplayStatus")
class DisplayStatusTest {

    @Nested
    @DisplayName("Order=CREATED + Payment=SUCCESS이면 ORDER_CONFIRMED")
    inner class OrderConfirmed {

        @Test
        @DisplayName("주문 확정 + 결제 성공 -> ORDER_CONFIRMED")
        fun of_orderCreatedPaymentSuccess() {
            val result = DisplayStatus.of(Order.Status.CREATED, Payment.Status.SUCCESS)
            assertThat(result).isEqualTo(DisplayStatus.ORDER_CONFIRMED)
        }
    }

    @Nested
    @DisplayName("Order=PENDING + Payment=PENDING이면 AWAITING_PAYMENT_RESULT")
    inner class AwaitingPaymentResult {

        @Test
        @DisplayName("주문 미확정 + 결제 대기 -> AWAITING_PAYMENT_RESULT")
        fun of_orderPendingPaymentPending() {
            val result = DisplayStatus.of(Order.Status.PENDING, Payment.Status.PENDING)
            assertThat(result).isEqualTo(DisplayStatus.AWAITING_PAYMENT_RESULT)
        }
    }

    @Nested
    @DisplayName("Order=PENDING + Payment=FAILED이면 REQUIRES_REPAYMENT")
    inner class RequiresRepayment {

        @Test
        @DisplayName("주문 미확정 + 결제 실패 -> REQUIRES_REPAYMENT")
        fun of_orderPendingPaymentFailed() {
            val result = DisplayStatus.of(Order.Status.PENDING, Payment.Status.FAILED)
            assertThat(result).isEqualTo(DisplayStatus.REQUIRES_REPAYMENT)
        }
    }

    @Nested
    @DisplayName("정의되지 않은 조합은 예외다")
    inner class InvalidCombination {

        @Test
        @DisplayName("Order=CREATED + Payment=PENDING -> 예외")
        fun of_unmappedCombination() {
            assertThrows<IllegalStateException> {
                DisplayStatus.of(Order.Status.CREATED, Payment.Status.PENDING)
            }
        }
    }
}
