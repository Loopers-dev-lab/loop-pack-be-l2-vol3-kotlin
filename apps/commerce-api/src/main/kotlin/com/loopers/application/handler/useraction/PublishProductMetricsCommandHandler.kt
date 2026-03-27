package com.loopers.application.handler.useraction

import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.domain.common.command.PublishProductMetricsCommand
import org.springframework.stereotype.Component

@Component
class PublishProductMetricsCommandHandler(
    private val outboxEventPublisher: OutboxEventPublisher,
) {
    fun handle(command: PublishProductMetricsCommand) {
        outboxEventPublisher.publish(
            aggregateType = "PRODUCT",
            aggregateId = command.targetId.toString(),
            eventType = "UserAction.${command.actionType}",
            payload = mapOf(
                "memberId" to command.memberId,
                "actionType" to command.actionType,
                "targetType" to command.targetType,
                "targetId" to command.targetId,
            ),
            partitionKey = command.targetId.toString(),
            topic = PRODUCT_ACTION_TOPIC,
        )
    }

    companion object {
        const val PRODUCT_ACTION_TOPIC = "product.action"
    }
}
