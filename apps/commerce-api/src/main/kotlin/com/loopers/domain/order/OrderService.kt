package com.loopers.domain.order

import com.loopers.domain.SnowflakeIdGenerator
import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderInfo
import com.loopers.domain.order.dto.OrderItemSpec
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.domain.order.event.OrderCreatedEvent
import com.loopers.domain.order.event.OrderLineItem
import com.loopers.domain.outbox.OutboxPublisher
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
    private val productService: ProductService,
    private val idGenerator: SnowflakeIdGenerator,
    private val outboxPublisher: OutboxPublisher,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun createOrder(userId: Long, items: List<CreateOrderItemCommand>, couponId: Long? = null): Order {
        validateItems(items)

        // Order 생성 (Snowflake ID 할당)
        val orderId = idGenerator.nextId()
        val order = Order.create(id = orderId, userId = userId, couponId = couponId)
        val savedOrder = orderRepository.save(order)

        // OrderItemSpec 준비
        val itemSpecs = items.map { cmd ->
            OrderItemSpec(
                product = productService.getProduct(cmd.productId),
                quantity = cmd.quantity,
                price = cmd.price,
            )
        }

        // 저장된 Order에 OrderItem 추가
        itemSpecs.forEach { spec ->
            savedOrder.addItem(spec.product, spec.quantity, spec.price)
        }

        // Publish OrderCreatedEvent to Outbox (same transaction)
        val lineItems = itemSpecs.map { spec ->
            OrderLineItem(
                productId = spec.product.id,
                quantity = spec.quantity,
            )
        }
        val event = OrderCreatedEvent(
            source = this,
            orderId = savedOrder.id,
            lineItems = lineItems,
        )
        outboxPublisher.publish(event, savedOrder.id)

        // Publish ApplicationEvent for local listeners
        eventPublisher.publishEvent(event)

        return savedOrder
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> {
        return orderRepository.findByUserId(userId, pageable).map { OrderedInfo.from(it) }
    }

    fun getOrderById(userId: Long, orderId: Long): Order =
        orderRepository.findById(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    fun getOrderByIdForAdmin(orderId: Long): Order =
        orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    fun getOrderByIdForUpdate(userId: Long, orderId: Long): Order =
        orderRepository.findByIdForUpdate(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문이 존재하지 않습니다")

    fun getOrderByIdForUpdateWithPending(userId: Long, orderId: Long): Order =
        orderRepository.findByIdForUpdateWithPending(orderId)
            ?.takeIf { it.userId == userId }
            ?: throw CoreException(ErrorType.BAD_REQUEST, "PENDING 상태의 주문이 아니거나 존재하지 않습니다")

    fun getOrderInfoForPayment(userId: Long, orderId: Long): OrderInfo {
        val order = getOrderByIdForUpdate(userId, orderId)

        // 상태 검증: PENDING이 아니면 이미 결제가 진행 중 또는 완료됨
        when (order.status) {
            OrderStatus.PAYMENT_REQUESTED ->
                throw CoreException(ErrorType.CONFLICT, "이미 결제가 진행 중입니다")
            OrderStatus.PENDING -> { /* 진행 */ }
            else ->
                throw CoreException(ErrorType.CONFLICT, "이미 완료된 주문입니다")
        }

        return OrderInfo(
            orderId = order.id,
            amount = order.getTotalPrice(),
        )
    }

    @Transactional
    fun saveOrder(order: Order): Order =
        orderRepository.save(order)

    @Transactional
    fun markOrderAsPaymentRequested(userId: Long, orderId: Long) {
        val order = getOrderByIdForUpdateWithPending(userId, orderId)
        order.markAsPaymentRequested()
    }

    @Transactional
    fun markOrderAsPaid(orderId: Long) {
        val order = getOrderByIdForAdmin(orderId)
        // ✅ 멱등성: 이미 PAID이면 무시
        if (order.status == OrderStatus.PAID) {
            return
        }
        order.markAsPaid() // ✅ 상태 검증 포함 (PAYMENT_REQUESTED만 가능)
    }

    @Transactional
    fun restoreOrderToPending(orderId: Long) {
        val order = getOrderByIdForAdmin(orderId)
        if (order.status == OrderStatus.PAYMENT_REQUESTED) {
            order.restoreToPending()
        }
        // 이미 PENDING이면 no-op (멱등성)
    }

    private fun validateItems(items: List<CreateOrderItemCommand>) {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다")
        }

        items.forEach { item ->
            if (item.quantity <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 0보다 커야 합니다")
            }
        }
    }
}
