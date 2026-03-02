package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.support.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneId

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    override fun save(order: Order): Order {
        return orderJpaRepository.save(order)
    }

    override fun findByIdOrNull(id: Long): Order? {
        return orderJpaRepository.findByIdOrNull(id)
    }

    override fun findByUserIdAndDateRange(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        page: Int,
        size: Int,
    ): PageResult<Order> {
        val zone = ZoneId.systemDefault()
        val startZoned = startDate.atStartOfDay(zone)
        val endZoned = endDate.plusDays(1).atStartOfDay(zone)

        val pageable = PageRequest.of(page, size)
        val result = orderJpaRepository.findByUserIdAndDateRange(userId, startZoned, endZoned, pageable)

        return PageResult.of(
            content = result.content,
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }

    override fun findAll(page: Int, size: Int): PageResult<Order> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "orderedAt"))
        val result = orderJpaRepository.findAll(pageable)

        return PageResult.of(
            content = result.content,
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }
}
