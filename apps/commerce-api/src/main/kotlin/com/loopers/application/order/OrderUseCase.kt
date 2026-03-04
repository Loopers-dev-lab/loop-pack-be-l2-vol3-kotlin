package com.loopers.application.order

import com.loopers.domain.coupon.CouponReader
import com.loopers.domain.coupon.IssuedCouponReader
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.order.OrderCanceller
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderReader
import com.loopers.domain.order.OrderRegister
import com.loopers.domain.product.ProductStockDeductor
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderUseCase(
    private val orderRegister: OrderRegister,
    private val orderReader: OrderReader,
    private val orderCanceller: OrderCanceller,
    private val productStockDeductor: ProductStockDeductor,
    private val couponReader: CouponReader,
    private val issuedCouponReader: IssuedCouponReader,
    private val issuedCouponRepository: IssuedCouponRepository,
) {

    @Transactional
    fun createOrder(memberId: Long, command: CreateOrderCommand): OrderInfo.Detail {
        val orderItems = command.items.map { item ->
            val product = productStockDeductor.deductStock(item.productId, item.quantity)
            OrderItem.from(product, item.quantity)
        }
        val totalPrice = orderItems.sumOf { it.subtotal }

        var discountAmount = 0L
        var usedCouponId: Long? = null
        if (command.couponId != null) {
            val issuedCoupon = issuedCouponReader.getByIdForUpdate(command.couponId)
            issuedCoupon.validateOwner(memberId)
            issuedCoupon.validateUsable()
            val coupon = couponReader.getById(issuedCoupon.couponId)
            if (coupon.isExpired()) throw CoreException(ErrorType.COUPON_EXPIRED)
            coupon.minOrderAmount.value?.let {
                if (totalPrice < it) throw CoreException(ErrorType.COUPON_MIN_ORDER_AMOUNT_NOT_MET)
            }
            discountAmount = coupon.calculateDiscount(totalPrice)
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)
            usedCouponId = issuedCoupon.id
        }

        val order = orderRegister.register(memberId, orderItems, discountAmount, usedCouponId)
        return OrderInfo.Detail.from(order)
    }

    @Transactional(readOnly = true)
    fun getById(orderId: Long, memberId: Long): OrderInfo.Detail {
        val order = orderReader.getById(orderId)
        order.validateOwner(memberId)
        return OrderInfo.Detail.from(order)
    }

    @Transactional(readOnly = true)
    fun getMyOrders(memberId: Long): List<OrderInfo.Main> {
        return orderReader.getAllByMemberId(memberId).map { OrderInfo.Main.from(it) }
    }

    @Transactional
    fun cancel(orderId: Long, memberId: Long) {
        val order = orderCanceller.cancel(orderId, memberId)
        order.orderItems.forEach { item ->
            productStockDeductor.restoreStock(item.productId, item.quantity)
        }
        if (order.couponId != null) {
            val issuedCoupon = issuedCouponReader.getById(order.couponId)
            issuedCoupon.restore()
            issuedCouponRepository.save(issuedCoupon)
        }
    }

    data class CreateOrderCommand(val items: List<OrderItemRequest>, val couponId: Long? = null)
    data class OrderItemRequest(val productId: Long, val quantity: Int)
}
