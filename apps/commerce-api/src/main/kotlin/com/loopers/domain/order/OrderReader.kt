package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class OrderReader(
    private val orderRepository: OrderRepository,
) {

    fun getById(id: Long): Order {
        return orderRepository.findById(id)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)
    }

    fun getAllByMemberId(memberId: Long): List<Order> {
        return orderRepository.findAllByMemberId(memberId)
    }
}
