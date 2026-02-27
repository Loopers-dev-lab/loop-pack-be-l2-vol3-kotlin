package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val userService: UserService,
) {
    @Transactional
    fun createOrder(loginId: String, password: String, itemRequests: List<OrderItemRequest>): OrderInfo {
        val user = getAuthenticatedUser(loginId, password)

        val sortedItems = itemRequests.sortedBy { it.productId }

        val orderItems = sortedItems.map { item ->
            val product = productService.getProductWithLock(item.productId)
            product.decreaseStock(item.quantity)
            OrderItem(
                productId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = item.quantity,
            )
        }

        val order = Order(userId = user.id, items = orderItems)
        return orderService.createOrder(order)
            .let { OrderInfo.from(it) }
    }

    fun getUserOrders(loginId: String, password: String, startAt: LocalDate, endAt: LocalDate): List<OrderInfo> {
        val user = getAuthenticatedUser(loginId, password)
        val startZoned = startAt.atStartOfDay(ZoneId.of("Asia/Seoul"))
        val endZoned = endAt.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul"))
        return orderService.getUserOrders(user.id, startZoned, endZoned)
            .map { OrderInfo.from(it) }
    }

    fun getOrder(loginId: String, password: String, orderId: Long): OrderInfo {
        val user = getAuthenticatedUser(loginId, password)
        val order = orderService.getOrder(orderId)
        if (order.userId != user.id) {
            throw CoreException(ErrorType.NOT_FOUND)
        }
        return OrderInfo.from(order)
    }

    fun getOrders(pageable: Pageable): Page<OrderInfo> {
        return orderService.getOrders(pageable)
            .map { OrderInfo.from(it) }
    }

    fun getOrderForAdmin(orderId: Long): OrderInfo {
        return orderService.getOrder(orderId)
            .let { OrderInfo.from(it) }
    }

    private fun getAuthenticatedUser(loginId: String, password: String) =
        userService.getUserByLoginIdAndPassword(loginId, password)
            ?: throw CoreException(ErrorType.NOT_FOUND, "User not found")
}

data class OrderItemRequest(
    val productId: Long,
    val quantity: Int,
)
