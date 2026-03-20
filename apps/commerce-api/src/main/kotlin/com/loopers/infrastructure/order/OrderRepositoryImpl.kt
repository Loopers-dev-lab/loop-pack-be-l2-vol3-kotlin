package com.loopers.infrastructure.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {
    override fun save(order: OrderModel): OrderModel {
        if (order.id == 0L) {
            return orderJpaRepository.save(OrderJpaModel.from(order)).toModel()
        }
        val existing = orderJpaRepository.findById(order.id).orElseThrow()
        // Order is immutable after creation, just return existing
        return existing.toModel()
    }

    override fun findById(id: Long): OrderModel? {
        return orderJpaRepository.findById(id).orElse(null)?.toModel()
    }

    override fun findAllByMemberIdAndOrderedAtBetween(
        memberId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
    ): List<OrderModel> {
        return orderJpaRepository.findAllByMemberIdAndOrderedAtBetween(memberId, startAt, endAt)
            .map { it.toModel() }
    }

    override fun findAll(pageQuery: PageQuery): PageResult<OrderModel> {
        val pageable = PageRequest.of(pageQuery.page, pageQuery.size)
        val page = orderJpaRepository.findAll(pageable)
        return PageResult(
            content = page.content.map { it.toModel() },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
