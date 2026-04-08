package com.loopers.domain.event

import com.loopers.infrastructure.event.OutboxEventJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OutboxEventServiceIntegrationTest @Autowired constructor(
    private val outboxEventService: OutboxEventService,
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("이벤트를 outbox 테이블에 저장하면, 커밋 후 즉시 Kafka 발행되어 published=true 상태가 된다.")
    @Test
    fun savesOutboxEvent_andPublishesImmediatelyAfterCommit() {
        // arrange
        val event = ProductLikedEvent(userId = 1L, productId = 100L)

        // act
        val saved = outboxEventService.saveOutboxEvent(
            aggregateType = "Product",
            aggregateId = "100",
            eventType = "ProductLiked",
            topic = "catalog-events",
            event = event,
        )

        // assert
        val found = outboxEventJpaRepository.findById(saved.id).get()
        assertAll(
            { assertThat(found.aggregateType).isEqualTo("Product") },
            { assertThat(found.aggregateId).isEqualTo("100") },
            { assertThat(found.eventType).isEqualTo("ProductLiked") },
            { assertThat(found.topic).isEqualTo("catalog-events") },
            { assertThat(found.published).isTrue() },
            { assertThat(found.payload).contains("productId") },
        )
    }

    @DisplayName("Kafka 발행에 실패한 이벤트는 published=false로 남아 스케줄러가 재시도할 수 있다.")
    @Test
    fun findsUnpublishedEvents_whenKafkaPublishFails() {
        // arrange — 직접 미발행 outbox 레코드를 생성 (Kafka 발행 실패 시뮬레이션)
        val unpublishedEvent = outboxEventJpaRepository.save(
            OutboxEvent(
                aggregateType = "Product",
                aggregateId = "999",
                eventType = "ProductLiked",
                payload = """{"userId":1,"productId":999}""",
                topic = "catalog-events",
            ),
        )

        // act
        val unpublished = outboxEventService.findUnpublishedEvents(100)

        // assert
        assertThat(unpublished).hasSize(1)
        assertThat(unpublished[0].aggregateId).isEqualTo("999")
    }
}
