package com.loopers.application.api.order

import com.loopers.application.api.order.dto.OrderItemCriteria
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.domain.product.ProductService
import com.loopers.interfaces.api.order.OrderV1Dto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
) {

    @Transactional
    fun createOrder(userId: Long, orderRequest: OrderV1Dto.OrderRequest): Long {
        val orderItems = orderRequest.orderItems.map { OrderItemCriteria(it.productId, it.quantity) }
        productService.decreaseProductsStock(orderItems)

        val createOrderItems = orderRequest.orderItems
            .map { orderItem ->
                val product = productService.getProduct(orderItem.productId)
                CreateOrderItemCommand(
                    productId = orderItem.productId,
                    productName = product.name,
                    quantity = orderItem.quantity,
                    price = product.price,
                )
            }
        return orderService.createOrder(userId, createOrderItems)
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> =
        orderService.getOrdersByUserId(userId, pageable)

    fun getOrderById(userId: Long, orderId: Long): OrderedInfo =
        orderService.getOrderById(userId, orderId).let { OrderedInfo.from(it) }
}
