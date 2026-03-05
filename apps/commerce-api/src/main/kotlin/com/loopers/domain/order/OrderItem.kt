package com.loopers.domain.order

import com.loopers.domain.common.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class OrderItem private constructor(
    val id: Long?,
    val snapshot: OrderSnapshot,
    val quantity: Quantity,
) {
    companion object {
        fun create(snapshot: OrderSnapshot, quantity: Quantity): OrderItem {
            if (quantity.value <= 0) {
                throw CoreException(ErrorType.INVALID_QUANTITY)
            }
            return OrderItem(id = null, snapshot = snapshot, quantity = quantity)
        }

        fun retrieve(id: Long, snapshot: OrderSnapshot, quantity: Quantity): OrderItem =
            OrderItem(id = id, snapshot = snapshot, quantity = quantity)
    }
}
