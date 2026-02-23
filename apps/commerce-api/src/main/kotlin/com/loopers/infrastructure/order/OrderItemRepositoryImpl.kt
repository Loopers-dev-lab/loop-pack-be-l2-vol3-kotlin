package com.loopers.infrastructure.order

import com.loopers.domain.order.entity.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface OrderItemJpaRepository : JpaRepository<OrderItemEntity, Long> {
    fun findAllByRefOrderId(refOrderId: Long): List<OrderItemEntity>
    fun findAllByRefOrderIdIn(refOrderIds: List<Long>): List<OrderItemEntity>
}

@Repository
class OrderItemRepositoryImpl(
    private val jpa: OrderItemJpaRepository,
) : OrderItemRepository {

    override fun save(orderItem: OrderItem): OrderItem {
        return jpa.save(OrderItemEntity.fromDomain(orderItem)).toDomain()
    }

    override fun saveAll(orderItems: List<OrderItem>): List<OrderItem> {
        return jpa.saveAll(orderItems.map { OrderItemEntity.fromDomain(it) }).map { it.toDomain() }
    }

    override fun findAllByOrderId(orderId: Long): List<OrderItem> {
        return jpa.findAllByRefOrderId(orderId).map { it.toDomain() }
    }

    override fun findAllByOrderIds(orderIds: List<Long>): List<OrderItem> {
        return jpa.findAllByRefOrderIdIn(orderIds).map { it.toDomain() }
    }
}
