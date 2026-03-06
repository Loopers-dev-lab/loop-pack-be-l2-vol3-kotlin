package com.loopers.domain.order

import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRegister(
    private val orderRepository: OrderRepository,
) {

    fun register(
        memberId: Long,
        orderItems: List<OrderItem>,
        totalPrice: Long = orderItems.sumOf { it.subtotal },
        discountAmount: Long = 0L,
        couponId: Long? = null,
    ): Order {
        val finalPrice = totalPrice - discountAmount

        val order = Order(
            memberId = memberId,
            orderItems = orderItems,
            totalPrice = totalPrice,
            discountAmount = discountAmount,
            finalPrice = finalPrice,
            couponId = couponId,
            orderedAt = ZonedDateTime.now(),
        )

        return orderRepository.save(order)
    }
}
