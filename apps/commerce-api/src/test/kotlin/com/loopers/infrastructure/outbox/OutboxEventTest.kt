package com.loopers.infrastructure.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("OutboxEvent")
class OutboxEventTest {

    @DisplayName("OutboxEvent를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("publishedAt은 null이다. (미발행 상태)")
        @Test
        fun publishedAtIsNull() {
            // arrange & act
            val event = OutboxEvent(
                aggregateType = "CATALOG",
                aggregateId = "100",
                eventType = "LIKED",
                version = 1L,
                payload = """{"userId":1,"productId":100}""",
            )

            // assert
            assertThat(event.publishedAt).isNull()
            assertThat(event.aggregateType).isEqualTo("CATALOG")
            assertThat(event.eventType).isEqualTo("LIKED")
        }
    }
}
