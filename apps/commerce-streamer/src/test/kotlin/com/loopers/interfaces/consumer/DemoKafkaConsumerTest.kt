package com.loopers.interfaces.consumer

import com.loopers.application.metrics.ProductMetricsEventHandler
import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.support.Acknowledgment
import java.time.ZonedDateTime

@DisplayName("DemoKafkaConsumer")
class DemoKafkaConsumerTest {
    private val productMetricsEventHandler: ProductMetricsEventHandler = mockk()
    private val demoKafkaConsumer = DemoKafkaConsumer(productMetricsEventHandler)

    @DisplayName("DB 처리 성공 후에만 manual ack를 호출한다")
    @Test
    fun acknowledgesAfterHandlerSuccess() {
        val message = catalogEventMessage(eventId = "event-1")
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)
        every { productMetricsEventHandler.handle(message) } just runs

        demoKafkaConsumer.catalogMetricsListener(
            messages = listOf(recordOf("product:10", message)),
            acknowledgment = acknowledgment,
        )

        verify(exactly = 1) { productMetricsEventHandler.handle(message) }
        verify(exactly = 1) { acknowledgment.acknowledge() }
    }

    @DisplayName("DB 처리 중 예외가 나면 manual ack를 호출하지 않는다")
    @Test
    fun doesNotAcknowledgeWhenHandlerFails() {
        val message = catalogEventMessage(eventId = "event-2")
        val acknowledgment = mockk<Acknowledgment>(relaxed = true)
        every { productMetricsEventHandler.handle(message) } throws IllegalStateException("db failure")

        assertThatThrownBy {
            demoKafkaConsumer.catalogMetricsListener(
                messages = listOf(recordOf("product:10", message)),
                acknowledgment = acknowledgment,
            )
        }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 0) { acknowledgment.acknowledge() }
    }

    private fun recordOf(
        key: String,
        event: CatalogEventMessage,
    ): ConsumerRecord<String, CatalogEventMessage> {
        return ConsumerRecord("catalog-events", 0, 0L, key, event)
    }

    private fun catalogEventMessage(eventId: String): CatalogEventMessage {
        return CatalogEventMessage(
            eventId = eventId,
            productId = 10L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 1,
            occurredAt = ZonedDateTime.now(),
        )
    }
}
