package com.loopers.infrastructure.order

import com.loopers.domain.order.AdminOrderRepository
import com.loopers.domain.order.Order
import com.loopers.support.page.PageRequest
import com.loopers.support.page.PageResponse
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime
import org.springframework.data.domain.PageRequest as SpringPageRequest

@Repository
class AdminOrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
    private val orderMapper: OrderMapper,
) : AdminOrderRepository {

    override fun findById(id: Long): Order? {
        val entity = orderJpaRepository.findByIdAndDeletedAtIsNull(id) ?: return null
        return orderMapper.toDomain(entity)
    }

    override fun findAll(
        from: ZonedDateTime?,
        to: ZonedDateTime?,
        pageRequest: PageRequest,
    ): PageResponse<Order> {
        val pageable = SpringPageRequest.of(
            pageRequest.page,
            pageRequest.size,
            Sort.by(Sort.Direction.DESC, "id"),
        )
        val page = orderJpaRepository.findAllByCreatedAtBetweenAndDeletedAtIsNull(
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
}
