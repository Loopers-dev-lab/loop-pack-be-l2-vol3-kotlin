package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.kafka.IntegrationEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

class KafkaIntegrationEventPublisherTest {
    private val kafkaTemplate = mockk<KafkaTemplate<String, String>>()
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val publisher = KafkaIntegrationEventPublisher(kafkaTemplate, objectMapper)

    @Test
    fun `통합 이벤트를 직렬화해서 카프카로 전송한다`() {
        every { kafkaTemplate.send(eq("catalog-events"), eq("1"), any()) } returns
            CompletableFuture.completedFuture(mockk<SendResult<String, String>>())

        publisher.publish(
            topic = "catalog-events",
            event = IntegrationEvent(
                eventId = "event-1",
                eventType = "ProductViewed",
                aggregateType = "product",
                aggregateId = "1",
                key = "1",
                version = 1L,
                occurredAt = ZonedDateTime.now(),
                payload = mapOf("productId" to 1L),
            ),
        )

        verify(exactly = 1) { kafkaTemplate.send(eq("catalog-events"), eq("1"), any()) }
    }
}
