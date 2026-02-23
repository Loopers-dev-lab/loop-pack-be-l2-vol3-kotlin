package com.loopers.application.order

import com.loopers.domain.PageResult
import com.loopers.domain.order.OrderService
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class GetOrdersUseCase(private val orderService: OrderService) {

    fun execute(userId: Long, from: ZonedDateTime, to: ZonedDateTime, page: Int, size: Int): PageResult<OrderInfo> {
        return orderService.getOrdersByUserId(userId, from, to, page, size)
            .map { OrderInfo.from(it) }
    }
}
