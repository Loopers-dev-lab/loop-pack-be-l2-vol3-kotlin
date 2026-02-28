package com.loopers.application.order

import com.loopers.application.common.PageResult
import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
@Transactional(readOnly = true)
class GetOrderUseCase(
    private val orderRepository: OrderRepository,
) {
    fun getById(userId: Long, orderId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: $orderId")

        order.assertOwnedBy(userId)

        return OrderInfo.from(order)
    }

    fun getByIdForAdmin(orderId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다: $orderId")
        return OrderInfo.from(order)
    }

    fun getAllByUserId(userId: Long, startAt: LocalDate, endAt: LocalDate, page: Int, size: Int): PageResult<OrderInfo> {
        val orders = orderRepository.findAllByUserIdAndOrderedDate(userId, startAt, endAt, page, size)
        val totalElements = orderRepository.countByUserIdAndOrderedDate(userId, startAt, endAt)
        val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
        return PageResult(
            content = orders.map { OrderInfo.from(it) },
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
        )
    }

    fun getAll(): List<OrderInfo> {
        return orderRepository.findAll().map { OrderInfo.from(it) }
    }
}
