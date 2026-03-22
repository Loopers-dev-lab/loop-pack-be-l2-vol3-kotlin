package com.loopers.application.event

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentEventListenerTest {

    private val listener = PaymentEventListener()

    @Nested
    @DisplayName("PaymentEvent.Completed 수신 시")
    inner class OnCompleted {

        @Test
        @DisplayName("예외 없이 정상 처리된다")
        fun handleCompleted_doesNotThrow() {
            // arrange
            val event = PaymentEvent.Completed(
                orderId = 1L,
                userId = 100L,
                totalAmount = 50000L,
            )

            // act & assert — 예외 없이 실행 완료
            listener.handleCompleted(event)
        }
    }

    @Nested
    @DisplayName("PaymentEvent.Failed 수신 시")
    inner class OnFailed {

        @Test
        @DisplayName("예외 없이 정상 처리된다")
        fun handleFailed_doesNotThrow() {
            // arrange
            val event = PaymentEvent.Failed(
                orderId = 1L,
                userId = 100L,
                reason = "잔액 부족",
            )

            // act & assert — 예외 없이 실행 완료
            listener.handleFailed(event)
        }
    }
}
