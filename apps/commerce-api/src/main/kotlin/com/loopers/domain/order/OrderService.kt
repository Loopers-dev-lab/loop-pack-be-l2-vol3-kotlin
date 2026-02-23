package com.loopers.domain.order

import com.loopers.domain.PageResult
import com.loopers.domain.common.Money
import com.loopers.domain.order.entity.Order
import com.loopers.domain.order.entity.OrderItem
import com.loopers.domain.order.repository.OrderItemRepository
import com.loopers.domain.order.repository.OrderRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Component
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional
    fun createOrder(userId: Long, products: List<OrderProductInfo>, command: OrderCommand.CreateOrder): OrderDetail {
        if (command.items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }

        val productMap = products.associateBy { it.id }

        val itemInfos = command.items.map { item ->
            val product = productMap[item.productId]
                ?: throw CoreException(ErrorType.BAD_REQUEST, "상품을 찾을 수 없습니다.")
            product to item.quantity
        }

        val totalPrice = itemInfos.fold(Money(BigDecimal.ZERO)) { acc, (product, quantity) ->
            acc + (product.price * quantity)
        }

        val order = Order.create(userId, totalPrice)
        val savedOrder = orderRepository.save(order)

        val orderItems = itemInfos.map { (product, quantity) ->
            OrderItem.create(product, quantity, savedOrder.id)
        }
        val savedItems = orderItemRepository.saveAll(orderItems)

        return OrderDetail(savedOrder, savedItems)
    }

    @Transactional(readOnly = true)
    fun getOrder(userId: Long, orderId: Long): OrderDetail {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.refUserId != userId) {
            throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        }
        val items = orderItemRepository.findAllByOrderId(orderId)
        return OrderDetail(order, items)
    }

    @Transactional(readOnly = true)
    fun getOrderForAdmin(orderId: Long): OrderDetail {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        val items = orderItemRepository.findAllByOrderId(orderId)
        return OrderDetail(order, items)
    }

    @Transactional(readOnly = true)
    fun getOrdersByUserId(userId: Long, from: ZonedDateTime, to: ZonedDateTime, page: Int, size: Int): PageResult<OrderDetail> {
        val pageResult = orderRepository.findAllByUserId(userId, from, to, page, size)
        val itemsByOrderId = findItemsByOrders(pageResult.content)
        return pageResult.map { order -> OrderDetail(order, itemsByOrderId[order.id] ?: emptyList()) }
    }

    @Transactional(readOnly = true)
    fun getAllOrders(page: Int, size: Int): PageResult<OrderDetail> {
        val pageResult = orderRepository.findAll(page, size)
        val itemsByOrderId = findItemsByOrders(pageResult.content)
        return pageResult.map { order -> OrderDetail(order, itemsByOrderId[order.id] ?: emptyList()) }
    }

    private fun findItemsByOrders(orders: List<Order>): Map<Long, List<OrderItem>> {
        if (orders.isEmpty()) return emptyMap()
        return orderItemRepository.findAllByOrderIds(orders.map { it.id })
            .groupBy { it.refOrderId }
    }
}
