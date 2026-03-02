package com.loopers.application.order

import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.support.PageResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class GetMyOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, startDate: LocalDate, endDate: LocalDate, page: Int, size: Int): PageResult<OrderSummaryInfo> {
        validatePeriod(startDate, endDate)

        val orderPage = orderRepository.findByUserIdAndDateRange(userId, startDate, endDate, page, size)

        if (orderPage.content.isEmpty()) {
            return PageResult.of(content = emptyList(), page = page, size = size, totalElements = orderPage.totalElements)
        }

        val orderIds = orderPage.content.map { it.id }
        val orderItems = orderItemRepository.findByOrderIds(orderIds)
        val itemCountMap = orderItems.groupBy { it.orderId }.mapValues { it.value.size }

        val summaries = orderPage.content.map { order ->
            OrderSummaryInfo.from(order, itemCountMap[order.id] ?: 0)
        }

        return PageResult.of(
            content = summaries,
            page = page,
            size = size,
            totalElements = orderPage.totalElements,
        )
    }

    private fun validatePeriod(startDate: LocalDate, endDate: LocalDate) {
        if (startDate.isAfter(endDate)) {
            throw CoreException(OrderErrorCode.INVALID_ORDER_PERIOD)
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            throw CoreException(OrderErrorCode.INVALID_ORDER_PERIOD)
        }
    }
}
