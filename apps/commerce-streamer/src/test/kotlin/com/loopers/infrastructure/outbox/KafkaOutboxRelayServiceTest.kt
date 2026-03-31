package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

@DisplayName("KafkaOutboxRelayService")
class KafkaOutboxRelayServiceTest {
    private val kafkaOutboxJpaRepository: KafkaOutboxJpaRepository = mock()
    private val kafkaTemplate: KafkaTemplate<String, KafkaOutboxEnvelope> = mock()
    private val objectMapper = jacksonObjectMapper()
    private val relayService = KafkaOutboxRelayService(kafkaOutboxJpaRepository, kafkaTemplate, objectMapper)

    @Test
    @DisplayName("publishedAt이 없는 outbox row를 Kafka로 발행하고 publishedAt을 채운다")
    fun relay() {
        org.mockito.kotlin.whenever(kafkaOutboxJpaRepository.findAllByPublishedAtIsNullOrderByIdAsc()).thenReturn(
            listOf(
                KafkaOutboxEntity(
                    id = 10L,
                    topic = "catalog-events",
                    eventKey = "100",
                    eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                    aggregateId = 100L,
                    payload = """{"productId":100}""",
                ),
            ),
        )
        whenever(kafkaTemplate.send(any(), any(), any())).thenReturn(
            CompletableFuture.completedFuture(mock<SendResult<String, KafkaOutboxEnvelope>>()),
        )
        whenever(kafkaOutboxJpaRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] as KafkaOutboxEntity }

        relayService.relay()

        val envelopeCaptor = argumentCaptor<KafkaOutboxEnvelope>()
        verify(kafkaTemplate).send(
            eq("catalog-events"),
            eq("100"),
            envelopeCaptor.capture(),
        )
        assertThat(envelopeCaptor.firstValue.eventId).isEqualTo(10L)
        assertThat(envelopeCaptor.firstValue.eventType).isEqualTo(KafkaEventType.PRODUCT_DETAIL_VIEWED)

        val outboxCaptor = argumentCaptor<KafkaOutboxEntity>()
        verify(kafkaOutboxJpaRepository).saveAndFlush(
            outboxCaptor.capture(),
        )
        assertThat(outboxCaptor.firstValue.publishedAt).isNotNull()
    }
}
