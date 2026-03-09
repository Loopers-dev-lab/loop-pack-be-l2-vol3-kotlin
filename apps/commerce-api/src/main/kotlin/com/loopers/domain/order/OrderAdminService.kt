package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderAdminService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    @Transactional(readOnly = true)
    fun getOrders(pageable: Pageable): Slice<OrderInfo> {
        val orderSlice = orderRepository.findAll(pageable)
        val orderIds = orderSlice.content.map { it.id }
        val itemsByOrderId = orderItemRepository.findAllByOrderIdIn(orderIds)
            .map { OrderItemInfo.from(it) }
            .groupBy { it.orderId }

        return orderSlice.map { order ->
            OrderInfo.from(order, itemsByOrderId[order.id] ?: emptyList())
        }
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        val items = orderItemRepository.findAllByOrderId(orderId)
            .map { OrderItemInfo.from(it) }
        return OrderInfo.from(order, items)
    }
}
