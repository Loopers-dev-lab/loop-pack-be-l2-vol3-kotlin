package com.loopers.application.handler.useraction

import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.domain.common.event.UserActionEvent
import com.loopers.domain.useraction.UserActionLogModel
import com.loopers.domain.useraction.UserActionLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class UserActionEventHandler(
    private val userActionLogRepository: UserActionLogRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: UserActionEvent) {
        userActionLogRepository.save(
            UserActionLogModel(
                memberId = event.memberId,
                actionType = event.actionType,
                targetType = event.targetType,
                targetId = event.targetId,
            ),
        )

        outboxEventPublisher.publish(
            aggregateType = "PRODUCT",
            aggregateId = event.targetId.toString(),
            eventType = "UserAction.${event.actionType.name}",
            payload = mapOf(
                "memberId" to event.memberId,
                "actionType" to event.actionType.name,
                "targetType" to event.targetType.name,
                "targetId" to event.targetId,
            ),
            partitionKey = event.targetId.toString(),
            topic = PRODUCT_ACTION_TOPIC,
        )

        log.debug(
            "유저 행동 로그 저장 + Outbox 발행: memberId={}, action={}, target={}:{}",
            event.memberId,
            event.actionType,
            event.targetType,
            event.targetId,
        )
    }

    companion object {
        const val PRODUCT_ACTION_TOPIC = "product.action"
    }
}
