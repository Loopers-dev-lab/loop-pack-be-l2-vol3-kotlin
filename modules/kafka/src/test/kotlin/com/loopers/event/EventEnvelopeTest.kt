package com.loopers.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("EventEnvelope")
class EventEnvelopeTest {

    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    @DisplayName("JSON 직렬화/역직렬화 시,")
    @Nested
    inner class JsonSerialization {

        @DisplayName("모든 필드가 보존된다.")
        @Test
        fun preservesAllFields() {
            // arrange
            val envelope = EventEnvelope(
                eventId = "evt-123",
                eventType = "LIKED",
                aggregateId = "100",
                version = 1L,
                timestamp = Instant.parse("2026-03-25T00:00:00Z"),
                payload = """{"userId":1,"productId":100}""",
            )

            // act
            val json = objectMapper.writeValueAsString(envelope)
            val deserialized = objectMapper.readValue(json, EventEnvelope::class.java)

            // assert
            assertThat(deserialized).isEqualTo(envelope)
        }
    }
}
