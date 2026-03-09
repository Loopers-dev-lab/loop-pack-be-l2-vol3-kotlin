package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.order.OrderService
import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserGetOrderUseCase(
    private val orderService: OrderService,
    private val userService: UserService,
) : UseCase<GetOrderCriteria, UserGetOrderResult> {

    @Transactional(readOnly = true)
    override fun execute(criteria: GetOrderCriteria): UserGetOrderResult {
        val user = userService.getUser(criteria.loginId)
        val info = orderService.getOrder(orderId = criteria.orderId, userId = user.id)
        return UserGetOrderResult.from(info)
    }
}
