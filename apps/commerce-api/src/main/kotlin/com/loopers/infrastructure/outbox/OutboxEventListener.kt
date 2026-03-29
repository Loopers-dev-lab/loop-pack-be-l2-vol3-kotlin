package com.loopers.infrastructure.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.coupon.event.CouponEvent
import com.loopers.domain.event.DomainEvent
import com.loopers.domain.like.event.LikeEvent
import com.loopers.domain.order.event.OrderEvent
import com.loopers.domain.product.event.ProductEvent
import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * 도메인 이벤트를 Outbox 테이블에 저장하는 리스너.
 *
 * BEFORE_COMMIT 시점에 동기로 실행되어 비즈니스 트랜잭션과 같은 TX에 묶인다.
 * → 트랜잭션 롤백 시 Outbox 저장도 함께 롤백 (원자성 보장)
 *
 * Kafka 발행 대상이 아닌 이벤트(USER_ACTION 등)는 무시한다.
 */
@Component
class OutboxEventListener(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log = LoggerFactory.getLogger(OutboxEventListener::class.java)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleOrderCreated(event: OrderEvent.Created) {
        saveOutbox(event)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductLiked(event: LikeEvent.Liked) {
        saveOutbox(event)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductUnliked(event: LikeEvent.Unliked) {
        saveOutbox(event)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleProductViewed(event: ProductEvent.Viewed) {
        saveOutbox(event)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun handleCouponIssueRequested(event: CouponEvent.IssueRequested) {
        saveOutbox(event)
    }

    private fun saveOutbox(event: DomainEvent) {
        if (event.eventType.topic.isBlank()) return // Kafka 발행 대상 아님

        try {
            val outboxEvent = OutboxEvent(
                eventId = event.eventId,
                eventType = event.eventType,
                aggregateId = event.aggregateId,
                topic = event.eventType.topic,
                partitionKey = event.aggregateId.toString(),
                payload = objectMapper.writeValueAsString(event),
            )
            outboxEventRepository.save(outboxEvent)
            log.debug("Outbox 저장 [eventId={}, type={}]", event.eventId, event.eventType)
        } catch (e: Exception) {
            log.error("Outbox 저장 실패 [eventId={}, type={}]", event.eventId, event.eventType, e)
            throw e // 같은 TX이므로 실패 시 비즈니스 트랜잭션도 롤백
        }
    }
}
