package com.loopers.application.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.UseCase
import com.loopers.application.outbox.OutboxEventPublisher
import com.loopers.config.kafka.message.OrderItemMessage
import com.loopers.config.kafka.message.OrderMessage
import com.loopers.domain.common.event.OrderCreatedEvent
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.CreateOrderItemCommand
import com.loopers.domain.order.OrderService
import com.loopers.domain.orderqueue.OrderQueueService
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.catalog.ProductMetricsRedisRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Component
class UserCreateOrderUseCase(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userService: UserService,
    private val orderQueueService: OrderQueueService,
    private val productMetricsRedisRepository: ProductMetricsRedisRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher,
) : UseCase<CreateOrderCriteria, CreateOrderResult> {

    @Transactional
    override fun execute(criteria: CreateOrderCriteria): CreateOrderResult {
        val user = userService.getUser(criteria.loginId)
        orderQueueService.validateAndConsumeToken(user.id)
        val couponDiscount = criteria.couponId?.let { issuedCouponId ->
            couponService.validateAndUseForOrder(issuedCouponId, user.id)
        }

        val command = CreateOrderCommand(
            userId = user.id,
            userName = criteria.loginId,
            items = criteria.items.map {
                CreateOrderItemCommand(productId = it.productId, quantity = it.quantity)
            },
            couponDiscount = couponDiscount,
            issuedCouponId = criteria.couponId,
        )
        val info = orderService.createOrder(command)

        info.items.forEach { item ->
            productMetricsRedisRepository.incrementOrderCount(item.productId)
        }

        val orderMessage = OrderMessage(
            eventId = UUID.randomUUID().toString(),
            orderId = info.id,
            userId = user.id,
            totalPrice = info.totalPrice,
            items = info.items.map { OrderItemMessage(it.productId, it.quantity, it.price) },
            occurredAt = ZonedDateTime.now(),
        )
        outboxEventPublisher.publish("ORDER", info.id, "ORDER_CREATED", objectMapper.writeValueAsString(orderMessage))

        eventPublisher.publishEvent(
            OrderCreatedEvent(
                orderId = info.id,
                userId = user.id,
                loginId = criteria.loginId,
                totalPrice = info.totalPrice,
            ),
        )

        return CreateOrderResult.from(info)
    }
}
