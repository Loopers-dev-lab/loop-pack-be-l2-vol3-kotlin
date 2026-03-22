package com.loopers.application.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentEventTest {

    @Nested
    @DisplayName("PaymentEvent.Completed 생성 시")
    inner class CompletedEvent {

        @Test
        @DisplayName("orderId, userId, totalAmount 필드를 포함한다")
        fun completed_hasRequiredFields() {
            // arrange & act
            val event = PaymentEvent.Completed(
                orderId = 1L,
                userId = 100L,
                totalAmount = 50000L,
            )

            // assert
            assertThat(event.orderId).isEqualTo(1L)
            assertThat(event.userId).isEqualTo(100L)
            assertThat(event.totalAmount).isEqualTo(50000L)
        }
    }

    @Nested
    @DisplayName("PaymentEvent.Failed 생성 시")
    inner class FailedEvent {

        @Test
        @DisplayName("orderId, userId, reason 필드를 포함한다")
        fun failed_hasRequiredFields() {
            // arrange & act
            val event = PaymentEvent.Failed(
                orderId = 1L,
                userId = 100L,
                reason = "잔액 부족",
            )

            // assert
            assertThat(event.orderId).isEqualTo(1L)
            assertThat(event.userId).isEqualTo(100L)
            assertThat(event.reason).isEqualTo("잔액 부족")
        }
    }
}
