package com.loopers.application.order

import com.loopers.domain.order.ImageUrl
import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderItemService
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.Price
import com.loopers.domain.order.ProductName
import com.loopers.domain.order.Quantity
import com.loopers.domain.product.ProductInventoryService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Component
class OrderFacade(
    private val orderService: OrderService,
    private val orderItemService: OrderItemService,
    private val productService: ProductService,
    private val productInventoryService: ProductInventoryService,
    private val userService: UserService,
) {
    @Transactional
    fun createOrder(loginId: String, items: List<OrderItemRequest>): OrderInfo {
        val user = userService.getUserByLoginId(loginId)

        val orderItems = items.map { req ->
            val product = productService.getProductById(req.productId)
            productInventoryService.decreaseStock(req.productId, req.quantity)
            Pair(product, req.quantity)
        }

        val totalAmount = orderItems.sumOf { (product, qty) -> product.price.value * qty }
        val order = orderService.createOrder(user.id, totalAmount)

        val savedItems = orderItemService.saveAll(
            orderItems.map { (product, qty) ->
                OrderItemModel(
                    orderId = order.id,
                    brandId = product.brandId,
                    productId = product.id,
                    quantity = Quantity(qty),
                    unitPrice = Price(product.price.value),
                    productName = ProductName(product.name.value),
                    imageUrl = ImageUrl(product.imageUrl.value),
                )
            }
        )
        return OrderInfo.from(order, savedItems)
    }

    fun getOrders(loginId: String, startAt: LocalDate, endAt: LocalDate, pageable: Pageable): Page<OrderInfo> {
        val user = userService.getUserByLoginId(loginId)
        val start = startAt.atStartOfDay(ZoneId.systemDefault())
        val end = endAt.plusDays(1).atStartOfDay(ZoneId.systemDefault())
        return orderService.getOrders(user.id, start, end, pageable).map { order ->
            val orderItems = orderItemService.getItemsByOrderId(order.id)
            OrderInfo.from(order, orderItems)
        }
    }

    fun getOrderById(loginId: String, orderId: Long): OrderInfo {
        val user = userService.getUserByLoginId(loginId)
        val order = orderService.getOrderById(orderId)
        if (order.userId != user.id) throw CoreException(ErrorType.FORBIDDEN)
        val orderItems = orderItemService.getItemsByOrderId(orderId)
        return OrderInfo.from(order, orderItems)
    }

    fun getAllOrders(pageable: Pageable): Page<OrderInfo> =
        orderService.getAllOrders(pageable).map { order ->
            val orderItems = orderItemService.getItemsByOrderId(order.id)
            OrderInfo.from(order, orderItems)
        }

    fun getAdminOrderById(orderId: Long): OrderInfo {
        val order = orderService.getOrderById(orderId)
        val orderItems = orderItemService.getItemsByOrderId(orderId)
        return OrderInfo.from(order, orderItems)
    }

    data class OrderItemRequest(
        val productId: Long,
        val quantity: Long,
    )
}
