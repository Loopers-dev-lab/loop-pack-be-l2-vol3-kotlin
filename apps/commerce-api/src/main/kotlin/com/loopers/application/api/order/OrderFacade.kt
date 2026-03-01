package com.loopers.application.api.order

import com.loopers.application.api.order.dto.OrderItemCriteria
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.DiscountDistributer
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.domain.product.ProductService
import com.loopers.interfaces.api.order.OrderV1Dto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional(readOnly = true)
class OrderFacade(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val couponService: CouponService,
) {

    @Transactional
    fun createOrder(userId: Long, orderRequest: OrderV1Dto.OrderRequest): Long {
        val orderItems = orderRequest.items.map { OrderItemCriteria(it.productId, it.quantity) }
        productService.decreaseProductsStock(orderItems)

        val createOrderItems = orderRequest.items
            .map { orderItem ->
                val product = productService.getProduct(orderItem.productId)
                CreateOrderItemCommand(
                    productId = orderItem.productId,
                    productName = product.name,
                    quantity = orderItem.quantity,
                    price = product.price,
                )
            }
        val orderId = orderService.createOrder(userId, createOrderItems, orderRequest.couponId)

        // 쿠폰 적용 (선택사항)
        if (orderRequest.couponId != null) {
            val order = orderService.getOrderByIdForAdmin(orderId)
            val totalAmount = order.getTotalPrice() + calculateTotalDiscount(order)

            try {
                val discount = couponService.calculateDiscount(orderRequest.couponId, totalAmount)
                DiscountDistributer.distributeDiscount(order.orderItems, discount, totalAmount)
                couponService.useCoupon(orderRequest.couponId, totalAmount)
            } catch (e: Exception) {
                throw e
            }
        }

        return orderId
    }

    private fun calculateTotalDiscount(order: Order): BigDecimal {
        return order.orderItems.sumOf { it.discountAmount }
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> =
        orderService.getOrdersByUserId(userId, pageable)

    fun getOrderById(userId: Long, orderId: Long): OrderedInfo =
        orderService.getOrderById(userId, orderId).let { OrderedInfo.from(it) }
}
