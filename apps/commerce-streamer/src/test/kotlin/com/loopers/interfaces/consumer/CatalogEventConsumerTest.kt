package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@DisplayName("CatalogEventConsumer")
class CatalogEventConsumerTest {

    private lateinit var updateProductMetricsUseCase: UpdateProductMetricsUseCase
    private lateinit var consumer: CatalogEventConsumer

    @BeforeEach
    fun setUp() {
        updateProductMetricsUseCase = mock()
        consumer = CatalogEventConsumer(updateProductMetricsUseCase)
    }

    private fun createRecord(value: Any?): ConsumerRecord<Any, Any> {
        return ConsumerRecord(CatalogEventConsumer.TOPIC, 0, 0L, null as Any?, value as Any?)
    }

    @Nested
    @DisplayName("정상 처리 시")
    inner class NormalCase {

        @Test
        fun `유효한 payload가 오면 handleCatalogEvent를 호출한다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "PRODUCT_CREATED",
                "productId" to 1L,
            )
            val record = createRecord(payload)

            // Act
            consumer.consume(record)

            // Assert
            verify(updateProductMetricsUseCase).handleCatalogEvent(
                eventId = "evt-1",
                eventType = "PRODUCT_CREATED",
                productId = 1L,
            )
        }
    }

    @Nested
    @DisplayName("페이로드 오류 시")
    inner class ErrorCase {

        @Test
        fun `payload가 null이면 IllegalArgumentException을 던진다`() {
            // Arrange
            val record = createRecord(null)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(IllegalArgumentException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }

        @Test
        fun `eventId가 누락되면 IllegalArgumentException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventType" to "PRODUCT_CREATED",
                "productId" to 1L,
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(IllegalArgumentException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }

        @Test
        fun `productId가 누락되면 IllegalArgumentException을 던진다`() {
            // Arrange
            val payload = mapOf(
                "eventId" to "evt-1",
                "eventType" to "PRODUCT_CREATED",
            )
            val record = createRecord(payload)

            // Act & Assert
            assertThatThrownBy { consumer.consume(record) }
                .isInstanceOf(IllegalArgumentException::class.java)
            verifyNoInteractions(updateProductMetricsUseCase)
        }
    }
}
