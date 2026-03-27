package com.loopers.infrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import java.util.concurrent.CompletableFuture

class DlqPublisherTest {

    private val kafkaTemplate: KafkaTemplate<Any, Any> = mock()
    private val dlqPublisher = DlqPublisher(kafkaTemplate)

    @DisplayName("DLQ 발행")
    @Nested
    inner class Publish {

        @DisplayName("원본 토픽에 .DLT를 붙여 DLQ 토픽으로 발행한다")
        @Test
        fun publishesToDlqTopic() {
            // arrange
            val record = ConsumerRecord("catalog-events", 0, 42L, "key-1", """{"data":"test"}""")
            record.headers().add(RecordHeader("eventId", "evt-001".toByteArray()))
            val exception = RuntimeException("처리 실패")

            val sendFuture = mock<java.util.concurrent.Future<Any>>()
            whenever(kafkaTemplate.send(org.mockito.kotlin.any<ProducerRecord<Any, Any>>())).thenReturn(CompletableFuture.completedFuture(null))

            // act
            dlqPublisher.publish(record, exception)

            // assert
            val captor = argumentCaptor<ProducerRecord<Any, Any>>()
            verify(kafkaTemplate).send(captor.capture())

            val sent = captor.firstValue
            assertAll(
                { assertThat(sent.topic()).`as`("DLQ 토픽은 {원본}.DLT").isEqualTo("catalog-events.DLT") },
                { assertThat(sent.key()).`as`("원본 키 유지").isEqualTo("key-1") },
                { assertThat(sent.value()).`as`("원본 값 유지").isEqualTo("""{"data":"test"}""") },
            )
        }

        @DisplayName("에러 정보 헤더가 포함된다")
        @Test
        fun includesErrorHeaders() {
            // arrange
            val record = ConsumerRecord("order-events", 0, 10L, "key-2", "{}")
            val exception = IllegalArgumentException("잘못된 형식")

            whenever(kafkaTemplate.send(org.mockito.kotlin.any<ProducerRecord<Any, Any>>())).thenReturn(CompletableFuture.completedFuture(null))

            // act
            dlqPublisher.publish(record, exception)

            // assert
            val captor = argumentCaptor<ProducerRecord<Any, Any>>()
            verify(kafkaTemplate).send(captor.capture())

            val headers = captor.firstValue.headers()
            assertAll(
                {
                    assertThat(String(headers.lastHeader("dlq.error.class").value()))
                        .`as`("에러 클래스명").isEqualTo("java.lang.IllegalArgumentException")
                },
                {
                    assertThat(String(headers.lastHeader("dlq.error.message").value()))
                        .`as`("에러 메시지").isEqualTo("잘못된 형식")
                },
                {
                    assertThat(headers.lastHeader("dlq.error.timestamp"))
                        .`as`("에러 발생 시각").isNotNull()
                },
                {
                    assertThat(String(headers.lastHeader("dlq.original.topic").value()))
                        .`as`("원본 토픽").isEqualTo("order-events")
                },
            )
        }

        @DisplayName("원본 헤더가 보존된다")
        @Test
        fun preservesOriginalHeaders() {
            // arrange
            val record = ConsumerRecord("catalog-events", 0, 0L, "key", "{}")
            record.headers().add(RecordHeader("eventId", "evt-original".toByteArray()))
            record.headers().add(RecordHeader("eventType", "LIKE_CREATED".toByteArray()))
            val exception = RuntimeException("오류")

            whenever(kafkaTemplate.send(org.mockito.kotlin.any<ProducerRecord<Any, Any>>())).thenReturn(CompletableFuture.completedFuture(null))

            // act
            dlqPublisher.publish(record, exception)

            // assert
            val captor = argumentCaptor<ProducerRecord<Any, Any>>()
            verify(kafkaTemplate).send(captor.capture())

            val headers = captor.firstValue.headers()
            assertAll(
                {
                    assertThat(String(headers.lastHeader("eventId").value()))
                        .`as`("원본 eventId 보존").isEqualTo("evt-original")
                },
                {
                    assertThat(String(headers.lastHeader("eventType").value()))
                        .`as`("원본 eventType 보존").isEqualTo("LIKE_CREATED")
                },
            )
        }
    }
}
