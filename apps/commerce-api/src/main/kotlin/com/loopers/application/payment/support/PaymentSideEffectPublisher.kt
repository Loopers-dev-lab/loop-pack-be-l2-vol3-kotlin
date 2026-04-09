package com.loopers.application.payment.support

import com.loopers.application.event.KafkaIntegrationEventPublisher
import com.loopers.application.event.PaymentStatusChangedEvent
import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentStatus
import com.loopers.kafka.IntegrationEvent
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.OrderPaidItemPayload
import com.loopers.kafka.OrderPaidPayload
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentSideEffectPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val kafkaIntegrationEventPublisher: KafkaIntegrationEventPublisher,
) {
    fun publish(order: Order, paymentStatus: PaymentStatus) {
        val orderId = requireNotNull(order.id)
        applicationEventPublisher.publishEvent(
            PaymentStatusChangedEvent(
                orderId = orderId,
                memberId = order.memberId,
                paymentStatus = paymentStatus.name,
            ),
        )
        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.PAYMENT_STATUS_CHANGED,
                memberId = order.memberId,
                targetType = "order",
                targetId = orderId.toString(),
                details = mapOf("paymentStatus" to paymentStatus.name),
            ),
        )

        if (order.status == OrderStatus.PAID) {
            kafkaIntegrationEventPublisher.publish(
                topic = KafkaTopics.ORDER_EVENTS,
                event = IntegrationEvent(
                    eventId = "order-paid:$orderId:1",
                    eventType = "OrderPaid",
                    aggregateType = "order",
                    aggregateId = orderId.toString(),
                    key = orderId.toString(),
                    version = 1L,
                    occurredAt = ZonedDateTime.now(),
                    payload = OrderPaidPayload(
                        orderId = orderId,
                        memberId = order.memberId,
                        items = order.orderItems.map { item ->
                            OrderPaidItemPayload(
                                productId = item.productId,
                                quantity = item.quantity,
                            )
                        },
                    ),
                ),
            )
        }
    }
}
