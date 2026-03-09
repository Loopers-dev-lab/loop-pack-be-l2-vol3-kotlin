package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderItemRepository
import org.springframework.stereotype.Component

@Component
class OrderItemRepositoryImpl(
    private val orderItemJpaRepository: OrderItemJpaRepository,
) : OrderItemRepository {
    override fun findAllByOrderId(orderId: Long): List<OrderItemModel> {
        return orderItemJpaRepository.findAllByOrderIdAndDeletedAtIsNull(orderId)
    }

    override fun findAllByOrderIdIn(orderIds: List<Long>): List<OrderItemModel> {
        return orderItemJpaRepository.findAllByOrderIdInAndDeletedAtIsNull(orderIds)
    }

    override fun save(item: OrderItemModel): OrderItemModel {
        return orderItemJpaRepository.save(item)
    }

    override fun saveAll(items: List<OrderItemModel>): List<OrderItemModel> {
        return orderItemJpaRepository.saveAll(items)
    }
}
