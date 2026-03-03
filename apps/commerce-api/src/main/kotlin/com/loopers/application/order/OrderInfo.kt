package com.loopers.application.order

import com.loopers.domain.order.model.OrderItem
import java.math.BigDecimal

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val status: String,
    val originalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
    val couponId: Long?,
    val items: List<OrderItemInfo>,
) {
    companion object {
        fun from(detail: OrderDetail): OrderInfo = OrderInfo(
            id = detail.order.id.value,
            userId = detail.order.refUserId.value,
            status = detail.order.status.name,
            originalPrice = detail.order.originalPrice.value,
            discountAmount = detail.order.discountAmount.value,
            totalPrice = detail.order.totalPrice.value,
            couponId = detail.order.refCouponId,
            items = detail.items.map { OrderItemInfo.from(it) },
        )
    }
}

data class OrderItemInfo(
    val id: Long,
    val productId: Long,
    val productName: String,
    val productPrice: BigDecimal,
    val quantity: Int,
    val status: String,
) {
    companion object {
        fun from(item: OrderItem): OrderItemInfo = OrderItemInfo(
            id = item.id,
            productId = item.refProductId.value,
            productName = item.productName,
            productPrice = item.productPrice.value,
            quantity = item.quantity.value,
            status = item.status.name,
        )
    }
}
