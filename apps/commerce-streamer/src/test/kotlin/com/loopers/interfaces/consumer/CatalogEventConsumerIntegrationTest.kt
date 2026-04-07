package com.loopers.interfaces.consumer

import com.loopers.event.KafkaEventMessage
import com.loopers.event.KafkaTopics
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
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
 * Kafka Consumer 설정 검증 통합 테스트.
 * Docker Compose Kafka와 연동하여 전체 흐름을 검증한다.
 *
 * 블로그용 검증 포인트:
 * 1. 멱등 처리: 같은 eventId 중복 발행 시 event_handled에서 skip
 * 2. manual ACK: offset이 처리 완료 후에만 커밋되는지 AdminClient로 확인
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

    @DisplayName("manual ACK offset 커밋 검증")
    @Nested
    inner class ManualAckOffsetVerification {

        @DisplayName("메시지 처리 전후로 committed offset이 정확히 증가한다")
        @Test
        fun committedOffsetAdvances_afterMessageProcessed() {
            // arrange — AdminClient로 처리 전 committed offset 기록
            val adminClient = createAdminClient()
            val consumerGroup = "metrics-consumer"
            val beforeOffsets = getCommittedOffsets(adminClient, consumerGroup)

            val productId = 100L
            val message = createLikedMessage(productId, version = 1L)

            // act — 메시지 발행 후 처리 대기
            val sendResult = kafkaTemplate.send(KafkaTopics.CATALOG_EVENTS, productId.toString(), message).get()
            val sentPartition = sendResult.recordMetadata.partition()
            val sentOffset = sendResult.recordMetadata.offset()

            await atMost Duration.ofSeconds(15) untilAsserted {
                val metrics = productMetricsJpaRepository.findByProductId(productId)
                assertThat(metrics).isNotNull
                assertThat(metrics!!.likeCount).isEqualTo(1)
            }

            // assert — 처리 후 committed offset이 발행된 메시지 이후로 이동했는지 확인
            val tp = TopicPartition(KafkaTopics.CATALOG_EVENTS, sentPartition)
            await atMost Duration.ofSeconds(10) untilAsserted {
                val afterOffsets = getCommittedOffsets(adminClient, consumerGroup)
                val committedOffset = afterOffsets[tp]?.offset() ?: 0
                assertThat(committedOffset).isGreaterThan(sentOffset)
            }

            adminClient.close()
        }

        @DisplayName("메시지를 발행하지 않으면 committed offset이 변하지 않는다")
        @Test
        fun committedOffsetUnchanged_whenNoMessagePublished() {
            // arrange
            val adminClient = createAdminClient()
            val consumerGroup = "metrics-consumer"

            // act — 아무 메시지도 발행하지 않고 5초 대기
            val beforeOffsets = getCommittedOffsets(adminClient, consumerGroup)
            Thread.sleep(5000)
            val afterOffsets = getCommittedOffsets(adminClient, consumerGroup)

            // assert — offset 변화 없음 (auto-commit이면 poll마다 커밋되지만, manual ACK은 변화 없음)
            for ((tp, beforeMeta) in beforeOffsets) {
                val afterMeta = afterOffsets[tp]
                assertThat(afterMeta?.offset()).isEqualTo(beforeMeta.offset())
            }

            adminClient.close()
        }

        private fun createAdminClient(): AdminClient {
            val props = mapOf("bootstrap.servers" to "localhost:19092")
            return AdminClient.create(props)
        }

        private fun getCommittedOffsets(
            adminClient: AdminClient,
            consumerGroup: String,
        ): Map<TopicPartition, OffsetAndMetadata> {
            return adminClient
                .listConsumerGroupOffsets(consumerGroup)
                .partitionsToOffsetAndMetadata()
                .get()
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
