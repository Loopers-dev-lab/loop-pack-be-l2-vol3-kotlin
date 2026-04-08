package com.loopers.application.payment.support

import com.loopers.application.event.OrderPaidEvent
import com.loopers.application.event.PaymentStatusChangedEvent
import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentStatus
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class PaymentSideEffectPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
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
            applicationEventPublisher.publishEvent(
                OrderPaidEvent(
                    orderId = orderId,
                    memberId = order.memberId,
                    items = order.orderItems.map { item ->
                        OrderPaidEvent.Item(
                            productId = item.productId,
                            quantity = item.quantity.toLong(),
                        )
                    },
                    occurredAt = ZonedDateTime.now(),
                ),
            )
        }
    }
}
