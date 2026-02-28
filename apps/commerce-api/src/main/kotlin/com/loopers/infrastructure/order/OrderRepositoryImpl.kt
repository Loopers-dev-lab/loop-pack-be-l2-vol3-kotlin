package com.loopers.infrastructure.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.ZoneId

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

    override fun findAllByUserIdAndOrderedDate(
        userId: Long,
        startAt: LocalDate,
        endAt: LocalDate,
        page: Int,
        size: Int,
    ): List<Order> {
        val zone = ZoneId.systemDefault()
        val startDateTime = startAt.atStartOfDay(zone)
        val endDateTime = endAt.plusDays(1).atStartOfDay(zone)
        val idPage = jpaRepository.findIdsByUserIdAndOrderedDateRange(
            userId,
            startDateTime,
            endDateTime,
            PageRequest.of(page, size),
        )
        if (idPage.isEmpty) return emptyList()
        return jpaRepository.findAllWithItemsByIdIn(idPage.content)
            .map { OrderMapper.toDomain(it) }
    }

    override fun countByUserIdAndOrderedDate(
        userId: Long,
        startAt: LocalDate,
        endAt: LocalDate,
    ): Long {
        val zone = ZoneId.systemDefault()
        val startDateTime = startAt.atStartOfDay(zone)
        val endDateTime = endAt.plusDays(1).atStartOfDay(zone)
        return jpaRepository.findIdsByUserIdAndOrderedDateRange(
            userId,
            startDateTime,
            endDateTime,
            PageRequest.of(0, 1),
        ).totalElements
    }

    override fun findAll(): List<Order> {
        return jpaRepository.findAllWithItems().map { OrderMapper.toDomain(it) }
    }
}
