package com.loopers.infrastructure.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("OutboxRelayScheduler")
class OutboxRelaySchedulerTest {

    @DisplayName("토픽 매핑 시,")
    @Nested
    inner class TopicMapping {

        @DisplayName("CATALOG aggregateType은 catalog-events 토픽으로 매핑된다.")
        @Test
        fun mapsCatalogToCatalogEvents() {
            // act
            val topic = OutboxRelayScheduler.topicFor("CATALOG")

            // assert
            assertThat(topic).isEqualTo("catalog-events")
        }

        @DisplayName("ORDER aggregateType은 order-events 토픽으로 매핑된다.")
        @Test
        fun mapsOrderToOrderEvents() {
            // act
            val topic = OutboxRelayScheduler.topicFor("ORDER")

            // assert
            assertThat(topic).isEqualTo("order-events")
        }
    }

    @DisplayName("EventEnvelope 변환 시,")
    @Nested
    inner class ToEnvelope {

        @DisplayName("OutboxEvent를 EventEnvelope로 변환한다.")
        @Test
        fun convertsOutboxEventToEnvelope() {
            // arrange
            val outboxEvent = OutboxEvent(
                id = 1L,
                aggregateType = "CATALOG",
                aggregateId = "100",
                eventType = "LIKED",
                version = 12345L,
                payload = """{"userId":1,"productId":100}""",
                createdAt = Instant.parse("2026-03-25T00:00:00Z"),
            )

            // act
            val envelope = OutboxRelayScheduler.toEventEnvelope(outboxEvent)

            // assert
            assertThat(envelope.eventType).isEqualTo("LIKED")
            assertThat(envelope.aggregateId).isEqualTo("100")
            assertThat(envelope.version).isEqualTo(12345L)
            assertThat(envelope.payload).isEqualTo("""{"userId":1,"productId":100}""")
            assertThat(envelope.eventId).isNotBlank()
        }
    }
}
