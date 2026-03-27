package com.loopers.interfaces.consumer

import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Kafka Consumer 멱등 처리 검증 통합 테스트.
 * Docker Compose Kafka와 연동하여 Kafka 발행 -> Consumer 수신 -> DB 반영 전체 흐름을 검증한다.
 *
 * 블로그용 검증 포인트:
 * 1. 같은 eventId 중복 발행 시 event_handled 테이블에서 skip -> 카운트 미증가
 * 2. 서로 다른 eventId 발행 시 각각 정상 처리 -> 카운트 증가
 */
@SpringBootTest
class CatalogEventConsumerIntegrationTest @Autowired constructor(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val eventHandledJpaRepository: EventHandledJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("멱등 처리 검증")
    @Nested
    inner class IdempotencyProcessing {

        @DisplayName("PRODUCT_LIKED 이벤트를 수신하면 ProductMetrics의 likeCount가 1 증가한다")
        @Test
        fun incrementsLikeCount_whenProductLikedEventReceived() {
            // arrange
            val productId = 1L
            val message = createLikedMessage(productId, version = 1L)

            // act
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), message)

            // assert
            await atMost Duration.ofSeconds(15) untilAsserted {
                val metrics = productMetricsJpaRepository.findByProductId(productId)
                assertThat(metrics).isNotNull
                assertThat(metrics!!.likeCount).isEqualTo(1)
            }
        }

        @DisplayName("같은 eventId로 중복 발행하면 likeCount가 1을 유지한다 (멱등 처리)")
        @Test
        fun likeCountRemainsOne_whenDuplicateEventIdPublished() {
            // arrange
            val productId = 2L
            val eventId = UUID.randomUUID().toString()
            val duplicateMessage = createLikedMessage(productId, version = 1L, eventId = eventId)
            val checkMessage = createViewedMessage(productId, version = 2L)

            // act - 1차 발행
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), duplicateMessage)

            // assert - 1차 처리 대기
            await atMost Duration.ofSeconds(15) untilAsserted {
                val metrics = productMetricsJpaRepository.findByProductId(productId)
                assertThat(metrics).isNotNull
                assertThat(metrics!!.likeCount).isEqualTo(1)
            }

            // act - 같은 eventId로 2차 발행(중복) + 확인용 PRODUCT_VIEWED 발행
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), duplicateMessage)
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), checkMessage)

            // assert - checkMessage(VIEWED)가 처리되었으면, 중복 메시지도 이미 처리(skip)됨
            await atMost Duration.ofSeconds(15) untilAsserted {
                val metrics = productMetricsJpaRepository.findByProductId(productId)
                assertThat(metrics).isNotNull
                assertThat(metrics!!.viewCount).isEqualTo(1)
                assertThat(metrics.likeCount).isEqualTo(1)
            }

            // event_handled 테이블에 원본 eventId 기록 확인
            assertThat(eventHandledJpaRepository.existsByEventId(eventId)).isTrue()
        }

        @DisplayName("서로 다른 eventId로 발행하면 likeCount가 각각 증가한다")
        @Test
        fun likeCountIncrements_whenDifferentEventIdsPublished() {
            // arrange
            val productId = 3L
            val message1 = createLikedMessage(productId, version = 1L)
            val message2 = createLikedMessage(productId, version = 2L)

            // act
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), message1)
            kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), message2)

            // assert
            await atMost Duration.ofSeconds(15) untilAsserted {
                val metrics = productMetricsJpaRepository.findByProductId(productId)
                assertThat(metrics).isNotNull
                assertThat(metrics!!.likeCount).isEqualTo(2)
            }
        }
    }

    private fun createLikedMessage(
        productId: Long,
        version: Long,
        eventId: String = UUID.randomUUID().toString(),
    ): KafkaEventMessage = KafkaEventMessage(
        eventId = eventId,
        eventType = "PRODUCT_LIKED",
        aggregateType = "PRODUCT",
        aggregateId = productId.toString(),
        payload = mapOf("userId" to 1, "productId" to productId, "liked" to true),
        version = version,
        occurredAt = ZonedDateTime.now(),
    )

    private fun createViewedMessage(
        productId: Long,
        version: Long,
        eventId: String = UUID.randomUUID().toString(),
    ): KafkaEventMessage = KafkaEventMessage(
        eventId = eventId,
        eventType = "PRODUCT_VIEWED",
        aggregateType = "PRODUCT",
        aggregateId = productId.toString(),
        payload = mapOf("userId" to 1, "productId" to productId),
        version = version,
        occurredAt = ZonedDateTime.now(),
    )
}
