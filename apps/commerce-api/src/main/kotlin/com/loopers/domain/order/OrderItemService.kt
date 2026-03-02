package com.loopers.domain.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderItemRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderItemService(
    private val orderItemRepository: OrderItemRepository,
) {
    @Transactional
    fun saveAll(items: List<OrderItemModel>): List<OrderItemModel> =
        orderItemRepository.saveAll(items)

    @Transactional(readOnly = true)
    fun getItemsByOrderId(orderId: Long): List<OrderItemModel> =
        orderItemRepository.findAllByOrderId(orderId)
}
