package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderItemRepository
import org.springframework.stereotype.Repository

@Repository
class OrderItemRepositoryImpl(
    private val orderItemJpaRepository: OrderItemJpaRepository
) : OrderItemRepository {
    override fun saveAll(items: List<OrderItemModel>): List<OrderItemModel> {
        return orderItemJpaRepository.saveAll(items)
    }

    override fun findAllByOrderId(orderId: Long): List<OrderItemModel> {
        return orderItemJpaRepository.findAllByOrderId(orderId)
    }

}
