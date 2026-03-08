package com.loopers.application.api.order

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.order.DiscountDistributer
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.CreateOrderItemCommand
import com.loopers.domain.order.dto.OrderedInfo
import com.loopers.domain.product.ProductService
import com.loopers.domain.stock.StockDecreaseCommand
import com.loopers.domain.stock.StockIncreaseCommand
import com.loopers.domain.stock.StockService
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
    private val stockService: StockService,
    private val couponService: CouponService,
) {

    @Transactional
    fun createOrder(userId: Long, orderRequest: OrderV1Dto.OrderRequest): Long {
        val sortedItems = orderRequest.items.sortedBy { it.productId }
        decreaseStock(sortedItems)

        try {
            val createOrderItems = prepareOrderItems(orderRequest)
            val discountAmount = applyCoupon(userId, orderRequest, createOrderItems)
            val order = orderService.createOrder(userId, createOrderItems, orderRequest.couponId)

            if (discountAmount > BigDecimal.ZERO) {
                applyDiscount(order, discountAmount, createOrderItems)
            }

            return order.id
        } catch (e: Exception) {
            increaseStock(sortedItems)
            throw e
        }
    }

    private fun decreaseStock(items: List<OrderV1Dto.OrderItemRequest>) {
        val decreaseCommands = items.map {
            StockDecreaseCommand(
                productId = it.productId,
                quantity = it.quantity,
            )
        }
        stockService.decreaseAllStocks(decreaseCommands)
    }

    private fun prepareOrderItems(orderRequest: OrderV1Dto.OrderRequest): List<CreateOrderItemCommand> {
        return orderRequest.items.map { orderItem ->
            val product = productService.getProduct(orderItem.productId)
            CreateOrderItemCommand(
                productId = orderItem.productId,
                productName = product.name,
                quantity = orderItem.quantity,
                price = product.price,
            )
        }
    }

    private fun applyCoupon(
        userId: Long,
        orderRequest: OrderV1Dto.OrderRequest,
        items: List<CreateOrderItemCommand>,
    ): BigDecimal {
        if (orderRequest.couponId == null) {
            return BigDecimal.ZERO
        }

        val totalAmount = items.sumOf { it.price * it.quantity.toBigDecimal() }
        val discountAmount = couponService.calculateDiscount(userId, orderRequest.couponId, totalAmount)
        couponService.useCoupon(userId, orderRequest.couponId, totalAmount)

        return discountAmount
    }

    private fun applyDiscount(order: Order, discountAmount: BigDecimal, items: List<CreateOrderItemCommand>) {
        val totalAmount = items.sumOf { it.price * it.quantity.toBigDecimal() }
        DiscountDistributer.distributeDiscount(order.orderItems, discountAmount, totalAmount)
    }

    private fun increaseStock(items: List<OrderV1Dto.OrderItemRequest>) {
        try {
            val increaseCommands = items.map {
                StockIncreaseCommand(
                    productId = it.productId,
                    quantity = it.quantity,
                )
            }
            stockService.increaseAllStocks(increaseCommands)
        } catch (ex: Exception) {
            throw ex
        }
    }

    fun getOrdersByUserId(userId: Long, pageable: Pageable): Page<OrderedInfo> =
        orderService.getOrdersByUserId(userId, pageable)

    fun getOrderById(userId: Long, orderId: Long): OrderedInfo =
        orderService.getOrderById(userId, orderId).let { OrderedInfo.from(it) }
}
