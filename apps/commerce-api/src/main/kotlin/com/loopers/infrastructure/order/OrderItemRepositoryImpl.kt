package com.loopers.infrastructure.order

import com.loopers.domain.order.entity.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository
import org.springframework.stereotype.Component

@Component
class OrderItemRepositoryImpl(
    private val orderItemJpaRepository: OrderItemJpaRepository,
) : OrderItemRepository {

    override fun save(orderItem: OrderItem): OrderItem {
        return orderItemJpaRepository.save(orderItem)
    }

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        return orderItemJpaRepository.saveAll(orderItems)
    }

    override fun findAllByOrderId(orderId: Long): List<OrderItem> {
        return orderItemJpaRepository.findAllByRefOrderId(orderId)
    }

    override fun findAllByOrderIds(orderIds: List<Long>): List<OrderItem> {
        return orderItemJpaRepository.findAllByRefOrderIdIn(orderIds)
    }
}
