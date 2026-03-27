package com.loopers.application.outbox

import com.loopers.infrastructure.outbox.OutboxEventPublisherImpl
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OutboxEventPublisher 테스트")
class OutboxEventPublisherTest {

    private lateinit var repository: FakeOutboxEventRepository
    private lateinit var publisher: OutboxEventPublisher

    @BeforeEach
    fun setUp() {
        repository = FakeOutboxEventRepository()
        publisher = OutboxEventPublisherImpl(repository, ObjectMapper())
    }

    @Test
    @DisplayName("publish 호출 시 outbox_event에 저장된다")
    fun `publish 호출 시 outbox에 저장된다`() {
        // act
        publisher.publish(
            aggregateType = "ORDER",
            aggregateId = "1",
            eventType = "PaymentSucceeded",
            payload = mapOf("orderId" to 1L),
            partitionKey = "1",
            topic = "payment.succeeded",
        )

        // assert
        val events = repository.findAll()
        assertThat(events).hasSize(1)
        assertThat(events[0].aggregateType).isEqualTo("ORDER")
        assertThat(events[0].eventType).isEqualTo("PaymentSucceeded")
        assertThat(events[0].topic).isEqualTo("payment.succeeded")
        assertThat(events[0].publishedAt).isNull()
    }

    @Test
    @DisplayName("저장된 이벤트의 ID는 UUID 형식이다")
    fun `저장된 이벤트의 ID는 UUID이다`() {
        // act
        publisher.publish(
            aggregateType = "PRODUCT",
            aggregateId = "100",
            eventType = "UserAction",
            payload = mapOf("actionType" to "VIEW", "targetId" to 100L),
            partitionKey = "100",
            topic = "product.action",
        )

        // assert
        val events = repository.findAll()
        assertThat(events[0].id).matches("[a-f0-9\\-]{36}")
    }

    @Test
    @DisplayName("미발행 이벤트를 조회할 수 있다")
    fun `미발행 이벤트를 조회할 수 있다`() {
        // arrange
        publisher.publish("A", "1", "EventA", mapOf("key" to "a"), "1", "topic.a")
        publisher.publish("B", "2", "EventB", mapOf("key" to "b"), "2", "topic.b")

        // act
        val unpublished = repository.findUnpublished(10)

        // assert
        assertThat(unpublished).hasSize(2)
    }

    @Test
    @DisplayName("발행 완료 마킹 후 미발행 조회에서 제외된다")
    fun `발행 완료 마킹 후 미발행 조회에서 제외된다`() {
        // arrange
        publisher.publish("A", "1", "EventA", mapOf("key" to "a"), "1", "topic.a")
        val event = repository.findAll().first()

        // act
        repository.markPublished(event.id)

        // assert
        val unpublished = repository.findUnpublished(10)
        assertThat(unpublished).isEmpty()
        assertThat(repository.findById(event.id)?.publishedAt).isNotNull()
    }
}
