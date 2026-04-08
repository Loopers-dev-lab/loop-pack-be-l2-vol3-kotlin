package com.loopers.application.order

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.event.EventTopics
import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.event.OutboxEventService
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.config.QueueProperties
import com.loopers.domain.queue.QueueService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.LocalDate
import java.time.ZoneId

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val userService: UserService,
    private val couponService: CouponService,
    private val queueService: QueueService,
    private val queueProperties: QueueProperties,
    private val eventPublisher: ApplicationEventPublisher,
    private val outboxEventService: OutboxEventService,
) {
    @Retry(name = "dbWrite")
    @CircuitBreaker(name = "database")
    @Transactional
    fun createOrder(
        loginId: String,
        password: String,
        itemRequests: List<OrderItemRequest>,
        couponId: Long? = null,
        queueToken: String? = null,
    ): OrderInfo {
        val user = getAuthenticatedUser(loginId, password)

        if (queueProperties.enabled) {
            if (queueToken == null || !queueService.validateToken(user.id, queueToken)) {
                throw CoreException(ErrorType.QUEUE_TOKEN_REQUIRED)
            }
        }

        val sortedItems = itemRequests.sortedBy { it.productId }

        val orderItems = sortedItems.map { item ->
            val product = productService.getProductWithLock(item.productId)
            product.decreaseStock(item.quantity)
            OrderItem(
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = item.quantity,
            )
        }

        val originalTotalPrice = orderItems.sumOf { it.productPrice * it.quantity }

        var discountAmount = 0L
        if (couponId != null) {
            val issuedCoupon = couponService.getIssuedCoupon(couponId)
            if (issuedCoupon.userId != user.id) {
                throw CoreException(ErrorType.BAD_REQUEST, "본인의 쿠폰만 사용할 수 있습니다.")
            }
            val template = couponService.getCouponTemplate(issuedCoupon.couponTemplateId)
            if (issuedCoupon.getStatus(template.expiredAt) != CouponStatus.AVAILABLE) {
                throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.")
            }
            if (template.minOrderAmount != null && originalTotalPrice < template.minOrderAmount!!) {
                throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액을 충족하지 못했습니다.")
            }
            discountAmount = template.calculateDiscount(originalTotalPrice)
            issuedCoupon.use()
        }

        val order = Order(userId = user.id, items = orderItems, couponId = couponId, discountAmount = discountAmount)
        val savedOrder = orderService.createOrder(order)

        val orderCreatedEvent = OrderCreatedEvent(
            orderId = savedOrder.id,
            userId = user.id,
            totalPrice = savedOrder.totalPrice,
            productIds = orderItems.map { it.productId },
        )
        eventPublisher.publishEvent(orderCreatedEvent)

        outboxEventService.saveOutboxEvent(
            aggregateType = "Order",
            aggregateId = savedOrder.id.toString(),
            eventType = "OrderCreated",
            topic = EventTopics.ORDER_EVENTS,
            event = orderCreatedEvent,
        )

        if (queueProperties.enabled) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() {
                        queueService.consumeToken(user.id)
                    }
                },
            )
        }

        return OrderInfo.from(savedOrder)
    }

    fun getUserOrders(loginId: String, password: String, startAt: LocalDate, endAt: LocalDate): List<OrderInfo> {
        val user = getAuthenticatedUser(loginId, password)
        val startZoned = startAt.atStartOfDay(ZoneId.of("Asia/Seoul"))
        val endZoned = endAt.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul"))
        return orderService.getUserOrders(user.id, startZoned, endZoned)
            .map { OrderInfo.from(it) }
    }

    fun getOrder(loginId: String, password: String, orderId: Long): OrderInfo {
        val user = getAuthenticatedUser(loginId, password)
        val order = orderService.getOrder(orderId)
        if (order.userId != user.id) {
            throw CoreException(ErrorType.NOT_FOUND)
        }
        return OrderInfo.from(order)
    }

    fun getOrders(pageable: Pageable): Page<OrderInfo> {
        return orderService.getOrders(pageable)
            .map { OrderInfo.from(it) }
    }

    fun getOrderForAdmin(orderId: Long): OrderInfo {
        return orderService.getOrder(orderId)
            .let { OrderInfo.from(it) }
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
}

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
