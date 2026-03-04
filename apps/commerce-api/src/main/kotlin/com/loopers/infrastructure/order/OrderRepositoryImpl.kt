package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import org.springframework.stereotype.Component

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderMapper: OrderMapper,
) : OrderRepository {

    override fun save(order: Order): Order {
        val entity = resolveEntity(order)
        val savedEntity = orderJpaRepository.save(entity)
        return orderMapper.toDomain(savedEntity)
    }

    override fun findById(id: Long): Order? {
        return orderJpaRepository.findById(id)
            .map { orderMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findAllByMemberId(memberId: Long): List<Order> {
        return orderJpaRepository.findAllByMemberId(memberId).map { orderMapper.toDomain(it) }
    }

    private fun resolveEntity(order: Order): OrderEntity {
        if (order.id == null) return orderMapper.toEntity(order)

        val entity = orderJpaRepository.getReferenceById(order.id)
        orderMapper.update(entity, order)
        return entity
    }
}
