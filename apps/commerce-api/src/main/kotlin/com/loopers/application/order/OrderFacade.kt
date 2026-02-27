package com.loopers.application.order

import com.loopers.domain.brand.BrandService
import com.loopers.domain.order.CreateOrderCommand
import com.loopers.domain.order.OrderInfo
import com.loopers.domain.order.OrderService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val orderService: OrderService,
) {

    @Transactional
    fun createOrder(userId: Long, criteria: CreateOrderCriteria): OrderResult {
        val productIds = criteria.items.map { it.productId }
        val products = productService.findByIds(productIds)
        if (products.size != productIds.toSet().size) {
            throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품이 포함되어 있습니다.")
        }

        val quantities = criteria.items.associate { it.productId to it.quantity }

        products.forEach { product ->
            val qty = quantities[product.id]!!
            productService.decreaseStock(product.id, qty)
        }

        val brandIds = products.map { it.brandId }.distinct()
        val brands = brandService.findByIds(brandIds).associateBy { it.id }

        val command = CreateOrderCommand(
            userId = userId,
            products = products,
            quantities = quantities,
            brands = brands,
        )
        val order = orderService.createOrder(command)

        return OrderResult.from(OrderInfo.from(order))
    }

    fun getOrders(userId: Long, criteria: GetOrdersCriteria): List<OrderResult> {
        val orders = orderService.findByUserIdAndCreatedAtBetween(userId, criteria.startAt, criteria.endAt)
        return orders.map { OrderResult.from(OrderInfo.from(it)) }
    }

    fun getOrder(userId: Long, orderId: Long): OrderResult {
        val order = orderService.findById(orderId)
        if (order.userId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "접근 권한이 없습니다.")
        }
        return OrderResult.from(OrderInfo.from(order))
    }

    fun getOrdersForAdmin(pageable: Pageable): Page<OrderResult> {
        return orderService.findAll(pageable)
            .map { OrderResult.from(OrderInfo.from(it)) }
    }

    fun getOrderForAdmin(orderId: Long): OrderResult {
        val order = orderService.findById(orderId)
        return OrderResult.from(OrderInfo.from(order))
    }
}
