package com.loopers.application.order

import com.loopers.domain.order.OrderItem

data class OrderItemInfo(
    val productName: String,
    val productPrice: Long,
    val brandName: String,
    val imageUrl: String,
    val quantity: Int,
) {
    companion object {
        fun from(item: OrderItem): OrderItemInfo {
            return OrderItemInfo(
                productName = item.productName,
                productPrice = item.productPrice.amount,
                brandName = item.brandName,
                imageUrl = item.imageUrl,
                quantity = item.quantity.value,
            )
        }
    }
}
