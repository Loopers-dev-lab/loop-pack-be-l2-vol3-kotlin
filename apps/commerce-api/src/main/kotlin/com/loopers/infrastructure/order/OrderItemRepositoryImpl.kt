package com.loopers.infrastructure.order

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.order.model.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface OrderItemJpaRepository : JpaRepository<OrderItemEntity, Long> {
    fun findAllByRefOrderId(refOrderId: Long): List<OrderItemEntity>
    fun findAllByRefOrderIdIn(refOrderIds: List<Long>): List<OrderItemEntity>
}

@Repository
class OrderItemRepositoryImpl(
    private val orderItemJpaRepository: OrderItemJpaRepository,
) : OrderItemRepository {

    override fun save(orderItem: OrderItem): OrderItem {
        return orderItemJpaRepository.save(OrderItemEntity.fromDomain(orderItem)).toDomain()
    }

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        return orderItemJpaRepository.saveAll(orderItems.map { OrderItemEntity.fromDomain(it) }).map { it.toDomain() }
    }

    override fun findAllByOrderId(orderId: OrderId): List<OrderItem> {
        return orderItemJpaRepository.findAllByRefOrderId(orderId.value).map { it.toDomain() }
    }

    override fun findAllByOrderIds(orderIds: List<OrderId>): List<OrderItem> {
        if (orderIds.isEmpty()) return emptyList()
        return orderItemJpaRepository.findAllByRefOrderIdIn(orderIds.map { it.value }).map { it.toDomain() }
    }
}
