package com.loopers.application.order

import com.loopers.application.event.UserActionLogEvent
import com.loopers.application.event.UserActionType
import com.loopers.domain.coupon.IssuedCouponProcessor
import com.loopers.domain.order.OrderCanceller
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRegister
import com.loopers.domain.product.ProductStockDeductor
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderUseCase(
    private val orderRegister: OrderRegister,
    private val orderReader: OrderReader,
    private val orderCanceller: OrderCanceller,
    private val productStockDeductor: ProductStockDeductor,
    private val issuedCouponProcessor: IssuedCouponProcessor,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun createOrder(memberId: Long, command: CreateOrderCommand): OrderInfo.Detail {
        val orderItems = command.items.map { item ->
            val product = productStockDeductor.deductStock(item.productId, item.quantity)
            OrderItem.from(product, item.quantity)
        }
        val totalPrice = orderItems.sumOf { it.subtotal }

        var discountAmount = 0L
        var usedCouponId: Long? = null
        if (command.couponId != null) {
            val reservation = issuedCouponProcessor.reserve(command.couponId, memberId, totalPrice)
            discountAmount = reservation.discountAmount
            usedCouponId = reservation.issuedCouponId
        }

        val order = orderRegister.register(memberId, orderItems, totalPrice, discountAmount, usedCouponId)
        applicationEventPublisher.publishEvent(
            UserActionLogEvent(
                actionType = UserActionType.ORDER_CREATED,
                memberId = memberId,
                targetType = "order",
                targetId = requireNotNull(order.id).toString(),
                details = mapOf(
                    "itemCount" to order.orderItems.size,
                    "finalPrice" to order.finalPrice,
                ),
            ),
        )
        return OrderInfo.Detail.from(order)
    }

    @Transactional(readOnly = true)
    fun getById(orderId: Long, memberId: Long): OrderInfo.Detail {
        val order = orderReader.getById(orderId)
        order.validateOwner(memberId)
        return OrderInfo.Detail.from(order)
    }

    @Transactional(readOnly = true)
    fun getMyOrders(memberId: Long): List<OrderInfo.Main> {
        return orderReader.getAllByMemberId(memberId).map { OrderInfo.Main.from(it) }
    }

    @Transactional
    fun cancel(orderId: Long, memberId: Long) {
        val result = orderCanceller.cancel(orderId, memberId)
        if (result.shouldRestoreStock) {
            result.order.orderItems.forEach { item ->
                productStockDeductor.restoreStock(item.productId, item.quantity)
            }
        }
        if (result.order.couponId != null) {
            issuedCouponProcessor.releaseIfReserved(result.order.couponId)
        }
    }

    data class CreateOrderCommand(val items: List<OrderItemRequest>, val couponId: Long? = null)
    data class OrderItemRequest(val productId: Long, val quantity: Int)
}
