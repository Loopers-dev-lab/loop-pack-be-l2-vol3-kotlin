package com.loopers.application.order

import com.loopers.domain.order.event.OrderCompletedEvent
import com.loopers.domain.order.event.OrderCompletedItem
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OrderEventHandler")
class OrderEventHandlerTest {

    private val handler = OrderEventHandler()

    @DisplayName("주문 완료 이벤트를 처리할 때,")
    @Nested
    inner class OnOrderCompleted {

        @DisplayName("예외 없이 로그를 출력한다.")
        @Test
        fun logsOrderCompletedEvent() {
            // arrange
            val event = OrderCompletedEvent(
                orderId = 1L,
                userId = 100L,
                items = listOf(
                    OrderCompletedItem(productId = 10L, quantity = 2, productName = "에어맥스"),
                ),
            )

            // act & assert — 예외 없이 정상 완료
            handler.onOrderCompleted(event)
        }
    }
}
