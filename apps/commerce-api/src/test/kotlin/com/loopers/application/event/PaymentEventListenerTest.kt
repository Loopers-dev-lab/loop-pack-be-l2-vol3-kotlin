package com.loopers.application.event

import com.loopers.domain.outbox.FakeOrderOutboxRepository
import com.loopers.domain.outbox.repository.OrderOutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PaymentEventListenerTest {

    private lateinit var orderOutboxRepository: OrderOutboxRepository
    private lateinit var listener: PaymentEventListener

    @BeforeEach
    fun setUp() {
        orderOutboxRepository = FakeOrderOutboxRepository()
        listener = PaymentEventListener(orderOutboxRepository)
    }

    @Nested
    @DisplayName("PaymentEvent.Completed 수신 시")
    inner class OnCompleted {

        @Test
        @DisplayName("OrderOutbox에 PAYMENT_COMPLETED로 기록된다")
        fun handleCompleted() {
            val event = PaymentEvent.Completed(
                orderId = 1L,
                userId = 100L,
                totalAmount = 50000L,
            )

            listener.handleCompleted(event)

            val outboxList = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxList).hasSize(1)
            assertThat(outboxList[0].eventType).isEqualTo("PAYMENT_COMPLETED")
            assertThat(outboxList[0].orderId).isEqualTo(1L)
            assertThat(outboxList[0].userId).isEqualTo(100L)
            assertThat(outboxList[0].totalAmount).isEqualTo(50000L)
        }
    }

    @Nested
    @DisplayName("PaymentEvent.Failed 수신 시")
    inner class OnFailed {

        @Test
        @DisplayName("OrderOutbox에 PAYMENT_FAILED로 기록된다")
        fun handleFailed() {
            val event = PaymentEvent.Failed(
                orderId = 1L,
                userId = 100L,
                reason = "잔액 부족",
            )

            listener.handleFailed(event)

            val outboxList = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxList).hasSize(1)
            assertThat(outboxList[0].eventType).isEqualTo("PAYMENT_FAILED")
            assertThat(outboxList[0].orderId).isEqualTo(1L)
            assertThat(outboxList[0].reason).isEqualTo("잔액 부족")
        }
    }
}
