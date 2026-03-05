package com.loopers.application.admin.order

import com.loopers.domain.order.AdminOrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminOrderDetailUseCase(
    private val adminOrderRepository: AdminOrderRepository,
) {
    @Transactional(readOnly = true)
    fun getDetail(orderId: Long): AdminOrderResult.Detail {
        val order = adminOrderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.ORDER_NOT_FOUND)
        return AdminOrderResult.Detail.from(order)
    }
}
