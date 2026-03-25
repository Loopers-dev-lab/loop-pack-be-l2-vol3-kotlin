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
                items = listOf(
                    PaymentEvent.Completed.OrderedProduct(productId = 10L, quantity = 2),
                    PaymentEvent.Completed.OrderedProduct(productId = 20L, quantity = 1),
                ),
            )

            listener.handleCompleted(event)

            val outboxList = orderOutboxRepository.findAllUnpublished()
            assertThat(outboxList).hasSize(2)
            assertThat(outboxList[0].eventType).isEqualTo("PAYMENT_COMPLETED")
            assertThat(outboxList[0].orderId).isEqualTo(1L)
            assertThat(outboxList[0].productId).isEqualTo(10L)
            assertThat(outboxList[0].quantity).isEqualTo(2)
            assertThat(outboxList[1].eventType).isEqualTo("PAYMENT_COMPLETED")
            assertThat(outboxList[1].orderId).isEqualTo(1L)
            assertThat(outboxList[1].productId).isEqualTo(20L)
            assertThat(outboxList[1].quantity).isEqualTo(1)
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
