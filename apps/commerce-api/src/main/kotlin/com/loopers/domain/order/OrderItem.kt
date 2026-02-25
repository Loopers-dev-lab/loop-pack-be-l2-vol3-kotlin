package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class OrderItem(
    val id: Long? = null,
    val productId: Long,
    val productName: String,
    val productPrice: Long,
    val quantity: Int,
) {
    init {
        if (quantity < 1) {
            throw CoreException(ErrorType.INVALID_ORDER_QUANTITY)
        }
    }

    val subtotal: Long
        get() = productPrice * quantity
}
