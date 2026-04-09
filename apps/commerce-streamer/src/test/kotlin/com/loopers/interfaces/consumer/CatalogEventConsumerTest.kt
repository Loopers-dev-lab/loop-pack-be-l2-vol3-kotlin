package com.loopers.interfaces.consumer

import com.loopers.application.metrics.UpdateProductMetricsUseCase
import com.loopers.interfaces.consumer.dto.CatalogEventPayload
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@DisplayName("CatalogEventConsumer")
class CatalogEventConsumerTest {

    private lateinit var updateProductMetricsUseCase: UpdateProductMetricsUseCase
    private lateinit var consumer: CatalogEventConsumer

    @BeforeEach
    fun setUp() {
        updateProductMetricsUseCase = mock()
        consumer = CatalogEventConsumer(updateProductMetricsUseCase)
    }

    @Test
    @DisplayName("유효한 payload가 오면 handleCatalogEvent를 호출한다")
    fun `유효한 payload가 오면 handleCatalogEvent를 호출한다`() {
        // Arrange
        val payload = CatalogEventPayload(
            eventId = "evt-1",
            eventType = "PRODUCT_CREATED",
            productId = 1L,
        )

        // Act
        consumer.consume(payload)

        // Assert
        verify(updateProductMetricsUseCase).handleCatalogEvent(
            eventId = "evt-1",
            eventType = "PRODUCT_CREATED",
            productId = 1L,
        )
    }
}
