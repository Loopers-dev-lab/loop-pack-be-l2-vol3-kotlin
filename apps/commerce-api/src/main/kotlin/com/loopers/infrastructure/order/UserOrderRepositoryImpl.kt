package com.loopers.infrastructure.order

import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Repository
class UserOrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderMapper: OrderMapper,
) : OrderRepository {

    override fun save(order: Order): Order {
        try {
            val savedEntity = orderJpaRepository.saveAndFlush(orderMapper.toEntity(order))
            return orderMapper.toDomain(savedEntity)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.ORDER_IDEMPOTENCY_KEY_DUPLICATE)
        }
    }

    override fun findById(id: Long): Order? {
        val entity = orderJpaRepository.findByIdAndDeletedAtIsNull(id) ?: return null
        return orderMapper.toDomain(entity)
    }

    override fun findByIdForUpdate(id: Long): Order? {
        val entity = orderJpaRepository.findByIdForUpdate(id) ?: return null
        return orderMapper.toDomain(entity)
    }

    override fun findByIdAndUserIdForUpdate(id: Long, userId: Long): Order? {
        val entity = orderJpaRepository.findByIdAndUserIdForUpdate(id, userId) ?: return null
        return orderMapper.toDomain(entity)
    }

    override fun findByIdAndUserId(id: Long, userId: Long): Order? {
        val entity = orderJpaRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId) ?: return null
        return orderMapper.toDomain(entity)
    }

    @Transactional(readOnly = true)
    override fun findAllByUserId(
        userId: Long,
        from: ZonedDateTime,
        toExclusive: ZonedDateTime,
        pageRequest: PageRequest,
    ): PageResponse<Order> {
        val pageable = SpringPageRequest.of(
            pageRequest.page,
            pageRequest.size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"),
            ),
        )
        val page = orderJpaRepository.findAllByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndDeletedAtIsNull(
            userId,
            from,
            toExclusive,
            pageable,
        )

        return PageResponse(
            content = page.content.map { orderMapper.toDomain(it) },
            totalElements = page.totalElements,
            page = pageRequest.page,
            size = pageRequest.size,
        )
    }

    @Transactional(readOnly = true)
    override fun findAll(pageRequest: PageRequest): PageResponse<Order> {
        val pageable = SpringPageRequest.of(
            pageRequest.page,
            pageRequest.size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id"),
            ),
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
