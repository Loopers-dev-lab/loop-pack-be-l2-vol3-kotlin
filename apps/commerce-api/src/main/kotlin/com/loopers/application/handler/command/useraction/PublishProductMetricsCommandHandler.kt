package com.loopers.application.handler.command.useraction

import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.domain.common.command.PublishProductMetricsCommand
import com.loopers.event.EventContract
import org.springframework.stereotype.Component

@Component
class PublishProductMetricsCommandHandler(
    private val outboxEventPublisher: OutboxEventPublisher,
) {
    fun handle(command: PublishProductMetricsCommand) {
        outboxEventPublisher.publish(
            aggregateType = EventContract.AGGREGATE_PRODUCT,
            aggregateId = command.targetId.toString(),
            eventType = "UserAction.${command.actionType}",
            payload = buildMap {
                put("memberId", command.memberId)
                put("actionType", command.actionType)
                put("targetType", command.targetType)
                put("targetId", command.targetId)
                putAll(command.metadata)
            },
            partitionKey = command.targetId.toString(),
            topic = EventContract.PRODUCT_ACTION_TOPIC,
        )
    }
}
