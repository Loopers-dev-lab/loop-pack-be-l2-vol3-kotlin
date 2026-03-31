package com.loopers.interfaces.consumer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.application.metric.KafkaEventEnvelope
import com.loopers.application.metric.KafkaMetricEventHandler
import com.loopers.infrastructure.outbox.KafkaEventType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.springframework.kafka.support.Acknowledgment

@DisplayName("KafkaMetricConsumer")
class KafkaMetricConsumerTest {
    private val objectMapper = jacksonObjectMapper()
    private val kafkaMetricEventHandler: KafkaMetricEventHandler = mock()
    private val consumer = KafkaMetricConsumer(objectMapper, kafkaMetricEventHandler)
    private val acknowledgment: Acknowledgment = mock()

    @Test
    @DisplayName("catalog-events record를 handler로 전달하고 ack한다")
    fun consume_catalogEvent() {
        val record = record(
            topic = "catalog-events",
            envelope = KafkaEventEnvelope(
                eventId = 1L,
                eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                aggregateId = 100L,
                payload = objectMapper.readTree("""{"productId":100}"""),
            ),
        )

        consumer.consume(listOf(record), acknowledgment)

        verify(kafkaMetricEventHandler).handle(
            eq("catalog-events"),
            check { envelope ->
                assertThat(envelope.eventId).isEqualTo(1L)
                assertThat(envelope.eventType).isEqualTo(KafkaEventType.PRODUCT_DETAIL_VIEWED)
            },
        )
        verify(acknowledgment).acknowledge()
    }

    @Test
    @DisplayName("payload 역직렬화가 실패하면 ack하지 않는다")
    fun consume_notAck_onDeserializationFailure() {
        val record = ConsumerRecord(
            "catalog-events",
            0,
            0L,
            "100",
            byteArrayOf(0x01, 0x02, 0x03),
        )

        assertThrows<Exception> {
            consumer.consume(listOf(record), acknowledgment)
        }

        verifyNoInteractions(kafkaMetricEventHandler)
        verify(acknowledgment, never()).acknowledge()
    }

    @Test
    @DisplayName("handler가 실패하면 ack하지 않는다")
    fun consume_notAck_onHandlerFailure() {
        val record = record(
            topic = "catalog-events",
            envelope = KafkaEventEnvelope(
                eventId = 1L,
                eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                aggregateId = 100L,
                payload = objectMapper.readTree("""{"productId":100}"""),
            ),
        )

        doThrow(RuntimeException("boom"))
            .whenever(kafkaMetricEventHandler)
            .handle(
                eq("catalog-events"),
                check { envelope ->
                    assertThat(envelope.eventId).isEqualTo(1L)
                    assertThat(envelope.eventType).isEqualTo(KafkaEventType.PRODUCT_DETAIL_VIEWED)
                },
            )

        assertThrows<RuntimeException> {
            consumer.consume(listOf(record), acknowledgment)
        }

        verify(acknowledgment, never()).acknowledge()
    }

    private fun record(
        topic: String,
        envelope: KafkaEventEnvelope,
    ): ConsumerRecord<String, ByteArray> =
        ConsumerRecord(
            topic,
            0,
            0L,
            envelope.aggregateId.toString(),
            objectMapper.writeValueAsBytes(envelope),
        )
}
