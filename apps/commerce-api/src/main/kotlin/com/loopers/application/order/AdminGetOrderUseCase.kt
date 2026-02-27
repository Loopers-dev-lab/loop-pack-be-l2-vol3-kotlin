package com.loopers.application.order

import com.loopers.application.UseCase
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderItemInfo
import com.loopers.domain.order.OrderItemRepository
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminGetOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val userRepository: UserRepository,
) : UseCase<Long, GetOrderDetailResult> {

    @Transactional(readOnly = true)
    override fun execute(orderId: Long): GetOrderDetailResult {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")

        val items = orderItemRepository.findAllByOrderId(orderId)
            .map { OrderItemResult.from(OrderItemInfo.from(it)) }

        val username = userRepository.find(order.userId)?.username ?: ""
        val info = OrderInfo.from(order)
        return GetOrderDetailResult.from(info, username, items)
    }
}
