package com.loopers.application.order

import com.loopers.domain.PageResult
import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetOrdersAdminUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(page: Int, size: Int): PageResult<OrderInfo> {
        val pageResult = orderRepository.findAll(page, size)
        val itemsByOrderId = orderItemRepository.findGroupedByOrderIds(pageResult.content)
        return pageResult.map { order -> OrderInfo.from(OrderDetail(order, itemsByOrderId[order.id] ?: emptyList())) }
    }
}
