package com.loopers.domain.order

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OrderModelPaymentTest {
    private fun createOrder(status: OrderStatus = OrderStatus.ORDERED) =
        OrderModel(memberId = 1L, status = status)

    @DisplayName("결제 요청 시 상태를 PAYMENT_PENDING으로 전이할 때,")
    @Nested
    inner class RequestPayment {
        @DisplayName("ORDERED 상태이면, PAYMENT_PENDING으로 전이된다.")
        @Test
        fun transitionsToPaymentPending_whenOrdered() {
            // arrange
            val order = createOrder(OrderStatus.ORDERED)

            // act
            val updated = order.requestPayment()

            // assert
            assertThat(updated.status).isEqualTo(OrderStatus.PAYMENT_PENDING)
        }

        @DisplayName("ORDERED 상태가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNotOrdered() {
            // arrange
            val order = createOrder(OrderStatus.PAID)

            // act & assert
            val result = assertThrows<CoreException> { order.requestPayment() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제 완료 시 상태를 PAID로 전이할 때,")
    @Nested
    inner class CompletePayment {
        @DisplayName("PAYMENT_PENDING 상태이면, PAID로 전이된다.")
        @Test
        fun transitionsToPaid_whenPaymentPending() {
            // arrange
            val order = createOrder(OrderStatus.PAYMENT_PENDING)

            // act
            val updated = order.completePayment()

            // assert
            assertThat(updated.status).isEqualTo(OrderStatus.PAID)
        }

        @DisplayName("PAYMENT_PENDING 상태가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNotPaymentPending() {
            // arrange
            val order = createOrder(OrderStatus.ORDERED)

            // act & assert
            val result = assertThrows<CoreException> { order.completePayment() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("결제 실패로 취소할 때,")
    @Nested
    inner class CancelByPaymentFailure {
        @DisplayName("PAYMENT_PENDING 상태이면, CANCELLED로 전이된다.")
        @Test
        fun transitionsToCancelled_whenPaymentPending() {
            // arrange
            val order = createOrder(OrderStatus.PAYMENT_PENDING)

            // act
            val updated = order.cancelByPaymentFailure()

            // assert
            assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        }

        @DisplayName("PAYMENT_PENDING 상태가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNotPaymentPending() {
            // arrange
            val order = createOrder(OrderStatus.ORDERED)

            // act & assert
            val result = assertThrows<CoreException> { order.cancelByPaymentFailure() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("ORDERED 상태로 되돌릴 때,")
    @Nested
    inner class RevertToOrdered {
        @DisplayName("PAYMENT_PENDING 상태이면, ORDERED로 전이된다.")
        @Test
        fun transitionsToOrdered_whenPaymentPending() {
            // arrange
            val order = createOrder(OrderStatus.PAYMENT_PENDING)

            // act
            val updated = order.revertToOrdered()

            // assert
            assertThat(updated.status).isEqualTo(OrderStatus.ORDERED)
        }

        @DisplayName("PAYMENT_PENDING 상태가 아니면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNotPaymentPending() {
            // arrange
            val order = createOrder(OrderStatus.PAID)

            // act & assert
            val result = assertThrows<CoreException> { order.revertToOrdered() }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
