package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.interfaces.consumer.dto.OrderEventPayload
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@DisplayName("OrderEventConsumer")
class OrderEventConsumerTest {

    private lateinit var updateProductMetricsUseCase: UpdateProductMetricsUseCase
    private lateinit var consumer: OrderEventConsumer

    @BeforeEach
    fun setUp() {
        updateProductMetricsUseCase = mock()
        consumer = OrderEventConsumer(updateProductMetricsUseCase)
    }

    @Nested
    @DisplayName("정상 처리 시")
    inner class NormalCase {

        @Test
        fun `유효한 payload가 오면 quantity를 포함해 handleOrderEvent를 호출한다`() {
            // Arrange
            val payload = OrderEventPayload(
                eventId = "evt-1",
                eventType = "PAYMENT_COMPLETED",
                productId = 1L,
                quantity = 3L,
            )

            // Act
            consumer.consume(payload)

            // Assert
            verify(updateProductMetricsUseCase).handleOrderEvent(
                eventId = "evt-1",
                eventType = "PAYMENT_COMPLETED",
                productId = 1L,
                quantity = 3L,
            )
        }

        @Test
        fun `quantity가 누락되면 기본값 1L로 handleOrderEvent를 호출한다`() {
            // Arrange
            val payload = OrderEventPayload(
                eventId = "evt-1",
                eventType = "PAYMENT_COMPLETED",
                productId = 1L,
            )

            // Act
            consumer.consume(payload)

            // Assert
            verify(updateProductMetricsUseCase).handleOrderEvent(
                eventId = "evt-1",
                eventType = "PAYMENT_COMPLETED",
                productId = 1L,
                quantity = 1L,
            )
        }
    }

    @Nested
    @DisplayName("비즈니스 검증 오류 시")
    inner class ValidationError {

        @Test
        fun `eventId가 공백이면 CoreException을 던진다`() {
            val payload = OrderEventPayload(
                eventId = "",
                eventType = "PAYMENT_COMPLETED",
                productId = 1L,
                quantity = 1L,
            )

            assertThatThrownBy { consumer.consume(payload) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }

        @Test
        fun `productId가 0 이하이면 CoreException을 던진다`() {
            val payload = OrderEventPayload(
                eventId = "evt-1",
                eventType = "PAYMENT_COMPLETED",
                productId = 0L,
                quantity = 1L,
            )

            assertThatThrownBy { consumer.consume(payload) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }

        @Test
        fun `quantity가 0 이하이면 CoreException을 던진다`() {
            val payload = OrderEventPayload(
                eventId = "evt-1",
                eventType = "PAYMENT_COMPLETED",
                productId = 1L,
                quantity = -1L,
            )

            assertThatThrownBy { consumer.consume(payload) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }
    }
}
