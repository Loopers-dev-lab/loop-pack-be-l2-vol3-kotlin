package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.order.OrderAdminService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminGetOrderUseCase(
    private val orderAdminService: OrderAdminService,
    private val userService: UserService,
) : UseCase<Long, GetOrderDetailResult> {

    @Transactional(readOnly = true)
    override fun execute(orderId: Long): GetOrderDetailResult {
        val orderInfo = orderAdminService.getOrder(orderId)
        val items = orderInfo.items.map { OrderItemResult.from(it) }
        val username = userService.findUserById(orderInfo.userId)?.username ?: ""
        return GetOrderDetailResult.from(orderInfo, username, items)
    }
}
