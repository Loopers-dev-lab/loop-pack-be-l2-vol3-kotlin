package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.config.kafka.event.CouponIssueRequestMessage
import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.ZonedDateTime
import java.util.concurrent.CompletableFuture

@DisplayName("OutboxRelayPublisher")
class OutboxRelayPublisherTest {
    private val outboxEventJpaRepository: OutboxEventJpaRepository = mockk()
    private val kafkaTemplate: KafkaTemplate<Any, Any> = mockk()
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private val outboxRelayPublisher = OutboxRelayPublisher(
        outboxEventJpaRepository = outboxEventJpaRepository,
        kafkaTemplate = kafkaTemplate,
        objectMapper = objectMapper,
        catalogTopic = "catalog-events",
        couponIssueRequestTopic = "coupon-issue-requests",
    )

    @DisplayName("relay는 outbox partition key를 그대로 사용해 발행한다")
    @Test
    fun publishesUsingStablePartitionKeys() {
        // arrange
        val firstMessage = CatalogEventMessage(
            eventId = "event-1",
            productId = 10L,
            eventType = CatalogEventType.LIKE_CHANGED,
            delta = 1,
            version = 100,
            occurredAt = ZonedDateTime.now().minusSeconds(1),
        )
        val secondMessage = CatalogEventMessage(
            eventId = "event-2",
            productId = 10L,
            eventType = CatalogEventType.PRODUCT_VIEWED,
            delta = 1,
            version = 101,
            occurredAt = ZonedDateTime.now(),
        )

        val firstOutbox = OutboxEventModel(
            eventId = firstMessage.eventId,
            topic = "catalog-events",
            partitionKey = "product:10",
            payload = objectMapper.writeValueAsString(firstMessage),
        )
        val secondOutbox = OutboxEventModel(
            eventId = secondMessage.eventId,
            topic = "catalog-events",
            partitionKey = "product:10",
            payload = objectMapper.writeValueAsString(secondMessage),
        )

        every { outboxEventJpaRepository.findTop100ByPublishedAtIsNullOrderByIdAsc() } returns listOf(firstOutbox, secondOutbox)
        every {
            kafkaTemplate.send(any(), any(), any())
        } returns CompletableFuture.completedFuture(mockk<SendResult<Any, Any>>())

        // act
        outboxRelayPublisher.publishPendingMessages()

        // assert
        verify(exactly = 2) {
            kafkaTemplate.send("catalog-events", "product:10", any<CatalogEventMessage>())
        }
        assertThat(firstOutbox.publishedAt).isNotNull
        assertThat(secondOutbox.publishedAt).isNotNull
    }

    @DisplayName("relay는 coupon issue request outbox도 발행한다")
    @Test
    fun publishesCouponIssueRequestEvent() {
        val message = CouponIssueRequestMessage(
            eventId = "event-3",
            requestId = 300L,
            couponId = 50L,
            userId = 77L,
            requestedAt = ZonedDateTime.now(),
        )

        val outbox = OutboxEventModel(
            eventId = message.eventId,
            topic = "coupon-issue-requests",
            partitionKey = "coupon:50",
            payload = objectMapper.writeValueAsString(message),
        )

        every { outboxEventJpaRepository.findTop100ByPublishedAtIsNullOrderByIdAsc() } returns listOf(outbox)
        every {
            kafkaTemplate.send(any(), any(), any())
        } returns CompletableFuture.completedFuture(mockk<SendResult<Any, Any>>())

        outboxRelayPublisher.publishPendingMessages()

        verify(exactly = 1) {
            kafkaTemplate.send("coupon-issue-requests", "coupon:50", any<CouponIssueRequestMessage>())
        }
        assertThat(outbox.publishedAt).isNotNull
    }
}
