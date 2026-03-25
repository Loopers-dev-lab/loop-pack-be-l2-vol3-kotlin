package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.support.error.CoreException
import org.apache.kafka.clients.consumer.ConsumerRecord
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

    private fun createRecord(value: Any?): ConsumerRecord<Any, Any> {
        return ConsumerRecord(OrderEventConsumer.TOPIC, 0, 0L, null as Any?, value as Any?)
    }

    @Nested
    @DisplayName("정상 처리 시")
    inner class NormalCase {

        @Test
        fun `유효한 payload가 오면 quantity를 포함해 handleOrderEvent를 호출한다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "PAYMENT_COMPLETED",
                "productId" to 1L,
                "quantity" to 3L,
            )
            val record = createRecord(payload)

            // Act
            consumer.consume(record)

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
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "PAYMENT_COMPLETED",
                "productId" to 1L,
            )
            val record = createRecord(payload)

            // Act
            consumer.consume(record)

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
    @DisplayName("페이로드 오류 시")
    inner class ErrorCase {

        @Test
        fun `payload가 null이면 CoreException을 던진다`() {
            // Arrange
            val record = createRecord(null)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }

        @Test
        fun `eventId가 누락되면 CoreException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventType" to "PAYMENT_COMPLETED",
                "productId" to 1L,
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }

        @Test
        fun `productId가 누락되면 CoreException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "PAYMENT_COMPLETED",
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(CoreException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }
    }
}
