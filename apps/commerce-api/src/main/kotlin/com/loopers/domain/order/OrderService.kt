package com.loopers.domain.order

import com.loopers.domain.catalog.ProductRepository
import com.loopers.domain.coupon.CouponDiscountInfo
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
class OrderService(
    private val userService: UserService,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    @Transactional
    fun createOrder(command: CreateOrderCommand): OrderInfo {
        val user = userService.getUser(command.loginId)

        val products = command.items.map { item ->
            val product = productRepository.findById(item.productId)
                ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다. (id: ${item.productId})")
            val decreased = productRepository.decreaseStock(item.productId, item.quantity)
            if (!decreased) throw CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.")
            product to item.quantity
        }

        val originalPrice = products.sumOf { (product, quantity) ->
            product.price * BigDecimal(quantity)
        }

        val discountAmount = command.couponDiscount?.calculateDiscount(originalPrice) ?: BigDecimal.ZERO
        val totalPrice = originalPrice.subtract(discountAmount).coerceAtLeast(BigDecimal.ONE)

        val order = orderRepository.save(
            OrderModel(
                userId = user.id,
                originalPrice = originalPrice,
                discountAmount = discountAmount,
                totalPrice = totalPrice,
                issuedCouponId = command.issuedCouponId,
            ),
        )

        val orderItems = products.map { (product, quantity) ->
            OrderItemModel(
                orderId = order.id,
                productId = product.id,
                productName = product.name,
                quantity = quantity,
                price = product.price,
            )
        }
        val savedItems = orderItemRepository.saveAll(orderItems)

        val itemInfos = savedItems.map { OrderItemInfo.from(it) }
        return OrderInfo.from(order, itemInfos)
    }

    @Transactional(readOnly = true)
    fun getOrders(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Slice<OrderInfo> {
        val orderSlice = orderRepository.findAllByUserIdAndCreatedAtBetween(userId, startAt, endAt, pageable)
        val orderIds = orderSlice.content.map { it.id }
        val itemsByOrderId = orderItemRepository.findAllByOrderIdIn(orderIds)
            .map { OrderItemInfo.from(it) }
            .groupBy { it.orderId }

        return orderSlice.map { order ->
            OrderInfo.from(order, itemsByOrderId[order.id] ?: emptyList())
        }
    }

    @Transactional
    fun cancelOrder(command: CancelOrderCommand): OrderInfo {
        val order = orderRepository.findById(command.orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.userId != command.userId) {
            throw CoreException(ErrorType.UNAUTHORIZED, "본인의 주문만 취소할 수 있습니다.")
        }

        order.cancel()

        val items = orderItemRepository.findAllByOrderId(order.id)
        items.forEach { item ->
            productRepository.increaseStock(item.productId, item.quantity)
        }

        val itemInfos = items.map { OrderItemInfo.from(it) }
        return OrderInfo.from(order, itemInfos)
    }

    @Transactional(readOnly = true)
    fun getOrder(orderId: Long, userId: Long): OrderInfo {
        val order = orderRepository.findById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
        if (order.userId != userId) {
            throw CoreException(ErrorType.UNAUTHORIZED, "본인의 주문만 조회할 수 있습니다.")
        }
        val items = orderItemRepository.findAllByOrderId(orderId)
            .map { OrderItemInfo.from(it) }
        return OrderInfo.from(order, items)
    }
}

data class CreateOrderCommand(
    val loginId: String,
    val items: List<CreateOrderItemCommand>,
    val couponDiscount: CouponDiscountInfo? = null,
    val issuedCouponId: Long? = null,
)

data class CreateOrderItemCommand(
    val productId: Long,
    val quantity: Int,
)

data class CancelOrderCommand(
    val orderId: Long,
    val userId: Long,
)
