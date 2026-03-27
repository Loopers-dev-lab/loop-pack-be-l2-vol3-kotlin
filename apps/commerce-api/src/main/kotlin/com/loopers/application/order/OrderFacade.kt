package com.loopers.application.order

import com.loopers.application.useraction.LogUserAction
import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val orderPreparationService: OrderPreparationService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    @LogUserAction(action = UserActionType.ORDER, targetType = UserActionTargetType.PRODUCT)
    @Transactional
    fun createOrder(memberId: Long, command: OrderCommand.Create) {
        val event = orderPreparationService.prepare(memberId, command)
        eventPublisher.publishEvent(event)
    }

    @Transactional(readOnly = true)
    fun getOrders(memberId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderInfo> {
        return orderService.getOrdersByMember(memberId, startAt, endAt)
            .map { OrderInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getOrder(memberId: Long, orderId: Long): OrderInfo {
        return orderService.getOrder(orderId, memberId)
            .let { OrderInfo.from(it) }
    }
}
