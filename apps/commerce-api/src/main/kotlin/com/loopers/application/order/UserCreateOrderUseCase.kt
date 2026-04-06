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
import com.loopers.infrastructure.order.OrderRateLimiter
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.UUID

@Component
class UserCreateOrderUseCase(
    private val orderService: OrderService,
    private val couponService: CouponService,
    private val userService: UserService,
    private val orderQueueService: OrderQueueService,
    private val orderRateLimiter: OrderRateLimiter,
    private val productMetricsRedisRepository: ProductMetricsRedisRepository,
    private val outboxEventPublisher: OutboxEventPublisher,
    private val objectMapper: ObjectMapper,
    private val eventPublisher: ApplicationEventPublisher,
    private val transactionTemplate: TransactionTemplate,
) : UseCase<CreateOrderCriteria, CreateOrderResult> {

    override fun execute(criteria: CreateOrderCriteria): CreateOrderResult {
        // 트랜잭션 밖 — Redis 작업, DB 커넥션 불필요
        orderRateLimiter.checkRate()
        val user = userService.getUser(criteria.loginId)

        // DB 작업만 트랜잭션으로 감싸기
        val info = transactionTemplate.execute {
            // 토큰 소비 (DEL 원자적 — TOCTOU 방지)
            orderQueueService.consumeTokenOrThrow(user.id)

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
            val orderInfo = orderService.createOrder(command)

            // Outbox INSERT — 주문과 같은 TX (원자성 보장)
            val orderMessage = OrderMessage(
                eventId = UUID.randomUUID().toString(),
                orderId = orderInfo.id,
                userId = user.id,
                totalPrice = orderInfo.totalPrice,
                items = orderInfo.items.map { OrderItemMessage(it.productId, it.quantity, it.price) },
                occurredAt = ZonedDateTime.now(),
            )
            outboxEventPublisher.publish("ORDER", orderInfo.id, "ORDER_CREATED", objectMapper.writeValueAsString(orderMessage))

            // @TransactionalEventListener(AFTER_COMMIT) 수신을 위해 TX 안에서 발행
            eventPublisher.publishEvent(
                OrderCreatedEvent(
                    orderId = orderInfo.id,
                    userId = user.id,
                    loginId = criteria.loginId,
                    totalPrice = orderInfo.totalPrice,
                ),
            )

            orderInfo
        }!!

        // 트랜잭션 밖 — Redis 후속 처리
        info.items.forEach { item ->
            productMetricsRedisRepository.incrementOrderCount(item.productId)
        }

        return CreateOrderResult.from(info)
    }
}
