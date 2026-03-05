package com.loopers.infrastructure.order

import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Repository
class UserOrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderMapper: OrderMapper,
) : OrderRepository {

    override fun save(order: Order): Order {
        val savedEntity = orderJpaRepository.saveAndFlush(orderMapper.toEntity(order))
        return orderMapper.toDomain(savedEntity)
    }

    override fun findById(id: Long): Order? {
        val entity = orderJpaRepository.findByIdAndDeletedAtIsNull(id) ?: return null
        return orderMapper.toDomain(entity)
    }

    override fun findByIdAndUserId(id: Long, userId: Long): Order? {
        val entity = orderJpaRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId) ?: return null
        return orderMapper.toDomain(entity)
    }

    override fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime?,
        to: ZonedDateTime?,
        pageRequest: PageRequest,
    ): PageResponse<Order> {
        val pageable = SpringPageRequest.of(
            pageRequest.page,
            pageRequest.size,
            Sort.by(Sort.Direction.DESC, "id"),
        )
        val page = orderJpaRepository.findAllByUserIdAndCreatedAtBetweenAndDeletedAtIsNull(
            userId,
            from!!,
            to!!,
            pageable,
        )

        return PageResponse(
            content = page.content.map { orderMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun findAll(pageRequest: PageRequest): PageResponse<Order> {
        val pageable = SpringPageRequest.of(
            pageRequest.page,
            pageRequest.size,
            Sort.by(Sort.Direction.DESC, "id"),
        )
        val page = orderJpaRepository.findAllByDeletedAtIsNull(pageable)

        return PageResponse(
            content = page.content.map { orderMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    override fun existsByIdempotencyKey(idempotencyKey: IdempotencyKey): Boolean {
        return orderJpaRepository.existsByIdempotencyKey(idempotencyKey.value)
    }
}
