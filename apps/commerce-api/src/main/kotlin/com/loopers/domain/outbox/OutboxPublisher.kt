package com.loopers.domain.outbox

import com.loopers.infrastructure.outbox.OutboxEvent
import com.loopers.infrastructure.outbox.OutboxRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDateTime

@Service
@Transactional
class OutboxPublisher(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val transactionManager: PlatformTransactionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 도메인 이벤트를 Outbox에 발행
     *
     * 동작:
     * 1. 현재 트랜잭션 내에서 Outbox 테이블에 저장
     * 2. 트랜잭션 커밋 후 afterCommit() 콜백에서 즉시 Kafka 발행
     * 3. 실패 시 OutboxPoller가 fallback으로 재발행
     *
     * @param event 발행할 도메인 이벤트
     * @param aggregateId aggregate 식별자
     * @param topic Kafka 토픽
     * @param partitionKey Kafka partition key (동시성 제어용, nullable)
     */
    fun publish(
        event: Any,
        aggregateId: Long,
        topic: String = "metrics-events",
        partitionKey: String? = null,
    ) {
        val payload = objectMapper.writeValueAsString(event)
        val saved = outboxRepository.save(
            OutboxEvent(
                aggregateId = aggregateId,
                eventType = event::class.simpleName!!,
                payload = payload,
                topic = topic,
                partitionKey = partitionKey,
            ),
        )

        // AfterCommit 콜백 등록: 트랜잭션 커밋 직후 Kafka 발행
        val key = partitionKey ?: aggregateId.toString()
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() {
                try {
                    kafkaTemplate.send(topic, key, payload).get()

                    // Kafka 발행 성공 → REQUIRES_NEW 트랜잭션으로 published=true 마킹
                    val definition = DefaultTransactionDefinition().apply {
                        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
                    }
                    val status = transactionManager.getTransaction(definition)
                    try {
                        saved.published = true
                        saved.publishedAt = LocalDateTime.now()
                        outboxRepository.save(saved)
                        transactionManager.commit(status)
                    } catch (e: Exception) {
                        transactionManager.rollback(status)
                        logger.error("Failed to mark outbox as published: id=${saved.id}", e)
                    }

                    logger.debug("Published outbox event via afterCommit: id=${saved.id}, eventType=${saved.eventType}")
                } catch (e: Exception) {
                    logger.warn(
                        "AfterCommit Kafka publish failed for outboxId=${saved.id}, " +
                            "eventType=${saved.eventType}, OutboxPoller will retry",
                        e,
                    )
                }
            }
        })
    }
}
