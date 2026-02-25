package com.loopers.application.order

import com.loopers.domain.PageResult
import com.loopers.domain.order.OrderDetail
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class GetOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, from: ZonedDateTime, to: ZonedDateTime, page: Int, size: Int): PageResult<OrderInfo> {
        val pageResult = orderRepository.findAllByUserId(userId, from, to, page, size)
        val itemsByOrderId = orderItemRepository.findGroupedByOrderIds(pageResult.content)
        return pageResult.map { order -> OrderInfo.from(OrderDetail(order, itemsByOrderId[order.id] ?: emptyList())) }
    }
}
