package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.loopers.config.kafka.event.CatalogEventMessage
import com.loopers.config.kafka.event.CatalogEventType
import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CatalogEventOutboxAppender - 주문 이벤트")
class CatalogEventOutboxAppenderOrderTest {
    private val outboxEventJpaRepository: OutboxEventJpaRepository = mockk()
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private val appender = CatalogEventOutboxAppender(
        outboxEventJpaRepository = outboxEventJpaRepository,
        objectMapper = objectMapper,
        catalogTopic = "catalog-events",
    )

    @DisplayName("주문 완료 이벤트 발행 시 상품별로 outbox 레코드가 저장된다")
    @Test
    fun savesOutboxRecordPerOrderItem() {
        // arrange
        val savedOutboxes = mutableListOf<OutboxEventModel>()
        every { outboxEventJpaRepository.save(any<OutboxEventModel>()) } answers {
            val outbox = firstArg<OutboxEventModel>()
            savedOutboxes.add(outbox)
            outbox
        }

        val event = OrderCompletedEvent(
            orderId = 1L,
            userId = 100L,
            totalAmount = 80000L,
            orderItems = listOf(
                OrderCompletedItem(productId = 10L, quantity = 2),
                OrderCompletedItem(productId = 20L, quantity = 1),
            ),
        )

        // act
        appender.appendOrderCompleted(event)

        // assert
        assertThat(savedOutboxes).hasSize(2)

        val firstPayload = objectMapper.readValue(savedOutboxes[0].payload, CatalogEventMessage::class.java)
        assertThat(firstPayload.productId).isEqualTo(10L)
        assertThat(firstPayload.eventType).isEqualTo(CatalogEventType.ORDER_COMPLETED)
        assertThat(firstPayload.delta).isEqualTo(2L)
        assertThat(savedOutboxes[0].topic).isEqualTo("catalog-events")
        assertThat(savedOutboxes[0].partitionKey).isEqualTo("product:10")

        val secondPayload = objectMapper.readValue(savedOutboxes[1].payload, CatalogEventMessage::class.java)
        assertThat(secondPayload.productId).isEqualTo(20L)
        assertThat(secondPayload.eventType).isEqualTo(CatalogEventType.ORDER_COMPLETED)
        assertThat(secondPayload.delta).isEqualTo(1L)
        assertThat(savedOutboxes[1].partitionKey).isEqualTo("product:20")
    }

    @DisplayName("주문 상품이 없으면 outbox 레코드가 저장되지 않는다")
    @Test
    fun doesNotSaveOutboxWhenNoOrderItems() {
        // arrange
        val savedCount = slot<OutboxEventModel>()
        every { outboxEventJpaRepository.save(capture(savedCount)) } answers { savedCount.captured }

        val event = OrderCompletedEvent(
            orderId = 1L,
            userId = 100L,
            totalAmount = 0L,
            orderItems = emptyList(),
        )

        // act
        appender.appendOrderCompleted(event)

        // assert
        assertThat(savedCount.isCaptured).isFalse()
    }
}
