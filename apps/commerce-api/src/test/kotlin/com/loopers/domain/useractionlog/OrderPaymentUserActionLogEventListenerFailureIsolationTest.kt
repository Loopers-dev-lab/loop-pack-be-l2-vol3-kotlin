package com.loopers.domain.useractionlog

import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.OrderLineItem
import com.loopers.domain.payment.event.PaymentCallbackProcessedEvent
import com.loopers.domain.payment.event.PaymentRequestedEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Order/Payment 사용자 액션 로그 이벤트 리스너 실패 격리")
class OrderPaymentUserActionLogEventListenerFailureIsolationTest {

    private val userActionLogPersistenceService = mockk<UserActionLogPersistenceService>()
    private val listener = OrderPaymentUserActionLogEventListener(userActionLogPersistenceService)

    @Test
    @DisplayName("OrderCreatedEvent 저장 실패는 리스너 내부에서 격리된다")
    fun isolatesOrderCreatedPersistenceFailure() {
        every { userActionLogPersistenceService.appendIfAbsent(any()) } throws RuntimeException("boom")

        assertThatCode {
            listener.onOrderCreated(
                OrderCreatedEvent(
                    orderId = 200L,
                    lineItems = listOf(
                        OrderLineItem(productId = 1L, quantity = 1),
                    ),
                    dedupeKey = "order.created:200",
                ),
            )
        }.doesNotThrowAnyException()

        verify(exactly = 1) { userActionLogPersistenceService.appendIfAbsent(any()) }
    }

    @Test
    @DisplayName("PaymentRequestedEvent 저장 실패는 리스너 내부에서 격리된다")
    fun isolatesPaymentRequestedPersistenceFailure() {
        every { userActionLogPersistenceService.appendIfAbsent(any()) } throws RuntimeException("boom")

        assertThatCode {
            listener.onPaymentRequested(
                PaymentRequestedEvent(
                    userId = 22L,
                    orderId = 201L,
                    transactionId = "txn-201",
                    amount = 5000L,
                    receiptStatus = "PENDING",
                    dedupeKey = "payment.requested:txn-201",
                ),
            )
        }.doesNotThrowAnyException()

        verify(exactly = 1) { userActionLogPersistenceService.appendIfAbsent(any()) }
    }

    @Test
    @DisplayName("PaymentCallbackProcessedEvent 저장 실패는 리스너 내부에서 격리된다")
    fun isolatesPaymentCallbackPersistenceFailure() {
        every { userActionLogPersistenceService.appendIfAbsent(any()) } throws RuntimeException("boom")

        assertThatCode {
            listener.onPaymentCallbackProcessed(
                PaymentCallbackProcessedEvent(
                    orderId = 202L,
                    status = "FAILED",
                    transactionId = "txn-202",
                    amount = 5000L,
                    reason = "denied",
                    dedupeKey = "payment.callback.processed:txn-202:FAILED",
                ),
            )
        }.doesNotThrowAnyException()

        verify(exactly = 1) { userActionLogPersistenceService.appendIfAbsent(any()) }
    }
}
