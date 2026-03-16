package com.loopers.application.order

import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.domain.payment.repository.PaymentRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetOrderUseCase(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val paymentRepository: PaymentRepository,
) {

    @Transactional(readOnly = true)
    fun execute(userId: Long, orderId: Long): OrderInfo {
        val order = orderRepository.findById(OrderId(orderId))
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        if (order.refUserId != UserId(userId)) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        val items = orderItemRepository.findAllByOrderId(OrderId(orderId))
        val payment = paymentRepository.findByOrderId(orderId)
        return OrderInfo.from(
            detail = OrderDetail(order, items),
            paymentStatus = payment?.status?.name,
            transactionKey = payment?.transactionKey,
        )
    }
}
