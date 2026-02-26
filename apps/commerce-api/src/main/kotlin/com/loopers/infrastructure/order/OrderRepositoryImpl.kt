package com.loopers.infrastructure.order

import com.loopers.domain.PageResult
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.model.Order
import com.loopers.domain.order.repository.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderEntity, Long> {
    fun findAllByRefUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
        refUserId: Long,
        from: ZonedDateTime,
        to: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderEntity>
    fun findAllByDeletedAtIsNull(pageable: Pageable): Page<OrderEntity>
}

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        return orderJpaRepository.save(OrderEntity.fromDomain(order)).toDomain()
    }

    override fun findById(id: OrderId): Order? {
        return orderJpaRepository.findById(id.value).orElse(null)?.toDomain()
    }

    override fun findAllByUserId(
        userId: UserId,
        from: ZonedDateTime,
        to: ZonedDateTime,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val pageable = PageRequest.of(page, size)
        val result = orderJpaRepository.findAllByRefUserIdAndCreatedAtBetweenAndDeletedAtIsNull(userId.value, from, to, pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }

    override fun findAll(page: Int, size: Int): PageResult<Order> {
        val pageable = PageRequest.of(page, size)
        val result = orderJpaRepository.findAllByDeletedAtIsNull(pageable)
        return PageResult(result.content.map { it.toDomain() }, result.totalElements, page, size)
    }
}
