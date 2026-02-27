package com.loopers.application.admin.order

import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.AdminOrderInfo
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminOrderFacade(
    private val orderService: OrderService,
    private val orderRepository: OrderRepository,
) {

    fun getOrders(pageable: Pageable): Page<AdminOrderInfo> =
        orderRepository.findOrders(pageable).map { AdminOrderInfo.from(it) }

    fun getOrderById(orderId: Long): AdminOrderInfo =
        orderService.getOrderByIdForAdmin(orderId).let { AdminOrderInfo.from(it) }
}
