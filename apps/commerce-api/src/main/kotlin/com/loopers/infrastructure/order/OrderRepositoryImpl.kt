package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import org.springframework.stereotype.Repository

@Repository
class OrderRepositoryImpl(
    private val jpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Long {
        val entity = OrderMapper.toEntity(order)
        val saved = jpaRepository.save(entity)
        return requireNotNull(saved.id) {
            "Order 저장 실패: id가 생성되지 않았습니다."
        }
    }

    override fun findById(id: Long): Order? {
        return jpaRepository.findByIdWithItems(id)?.let { OrderMapper.toDomain(it) }
    }

    override fun findByIdForUpdate(id: Long): Order? {
        return jpaRepository.findByIdForUpdate(id)?.let { OrderMapper.toDomain(it) }
    }

    override fun findAllByUserId(userId: Long): List<Order> {
        return jpaRepository.findAllByUserIdWithItems(userId).map { OrderMapper.toDomain(it) }
    }

    override fun findAll(): List<Order> {
        return jpaRepository.findAllWithItems().map { OrderMapper.toDomain(it) }
    }
}
