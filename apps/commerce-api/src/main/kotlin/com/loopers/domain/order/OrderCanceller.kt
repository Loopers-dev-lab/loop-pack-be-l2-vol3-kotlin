package com.loopers.domain.order

import org.springframework.stereotype.Component

@Component
class OrderCanceller(
    private val orderReader: OrderReader,
    private val orderRepository: OrderRepository,
) {

    fun cancel(orderId: Long, memberId: Long): CancelResult {
        val order = orderReader.getByIdForUpdate(orderId)
        order.validateOwner(memberId)
        val shouldRestoreStock = order.status != OrderStatus.PAYMENT_FAILED
        order.cancel()
        val cancelledOrder = orderRepository.save(order)
        return CancelResult(
            order = cancelledOrder,
            shouldRestoreStock = shouldRestoreStock,
        )
    }

    data class CancelResult(
        val order: Order,
        val shouldRestoreStock: Boolean,
    )
}
