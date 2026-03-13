package com.loopers.application.order

import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.PageResult
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.product.ProductModel
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
) {
    fun createOrder(
        memberId: Long,
        products: Map<Long, ProductModel>,
        brandNames: Map<Long, String>,
        items: List<OrderCommand.CreateOrderItem>,
        couponId: Long? = null,
        discountAmount: Long = 0,
    ): OrderModel {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.")
        }
        if (items.any { it.quantity <= 0 }) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        }

        var order = OrderModel(memberId = memberId, couponId = couponId, discountAmount = discountAmount)
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
            order = order.addItem(orderItem)
        }
        return orderRepository.save(order)
    }

    fun getOrder(orderId: Long, memberId: Long): OrderModel {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.")
        order.validateOwner(memberId)
        return order
    }

    fun getOrdersByMember(memberId: Long, startAt: ZonedDateTime, endAt: ZonedDateTime): List<OrderModel> {
        return orderRepository.findAllByMemberIdAndOrderedAtBetween(memberId, startAt, endAt)
    }

    fun getOrderById(orderId: Long): OrderModel {
        return orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다.")
    }

    fun getOrders(page: Int, size: Int): PageResult<OrderModel> {
        return orderRepository.findAll(PageQuery(page, size))
    }
}
