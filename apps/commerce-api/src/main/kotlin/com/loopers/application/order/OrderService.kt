package com.loopers.application.order

import com.loopers.domain.order.OrderCommand
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.product.ProductModel
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun createOrder(
        memberId: Long,
        products: Map<Long, ProductModel>,
        brandNames: Map<Long, String>,
        items: List<OrderCommand.CreateOrderItem>,
    ): OrderModel {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.")
        }
        if (items.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        }

        val order = OrderModel(memberId = memberId)
        items.forEach { item ->
            val product = products[item.productId]
                ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
            val brandName = brandNames[product.brandId] ?: ""
            val orderItem = OrderItemModel(
                productId = item.productId,
                productName = product.name,
                productPrice = product.price,
                brandName = brandName,
                quantity = item.quantity,
            )
            order.addItem(orderItem)
        }
        return orderRepository.save(order)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long, memberId: Long): OrderModel {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.")
        order.validateOwner(memberId)
        return order
    }

    @Transactional(readOnly = true)
    fun getOrdersByMember(memberId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderModel> {
        return orderRepository.findAllByMemberIdAndOrderedAtBetween(memberId, startAt, endAt)
    }

    @Transactional(readOnly = true)
    fun getOrderById(orderId: Long): OrderModel {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.")
    }

    @Transactional(readOnly = true)
    fun getOrders(page: Int, size: Int): Page<OrderModel> {
        return orderRepository.findAll(PageRequest.of(page, size))
    }
}
