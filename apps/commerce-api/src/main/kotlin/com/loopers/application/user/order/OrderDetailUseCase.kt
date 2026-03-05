package com.loopers.application.user.order

import com.loopers.domain.order.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderDetailUseCase(
    private val orderRepository: OrderRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(orderId: Long, userId: Long): OrderResult.Detail {
        val order = orderRepository.findByIdAndUserId(orderId, userId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)
        return OrderResult.Detail.from(order)
    }
}
