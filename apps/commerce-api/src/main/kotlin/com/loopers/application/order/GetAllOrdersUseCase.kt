package com.loopers.application.order

import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.support.PageResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetAllOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int): PageResult<OrderSummaryInfo> {
        val orderPage = orderRepository.findAll(page, size)

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
}
