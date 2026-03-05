package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Order private constructor(
    val id: Long?,
    val userId: Long,
    val idempotencyKey: IdempotencyKey,
    val status: Status,
    val items: List<OrderItem>,
) {
    init {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.ORDER_INVALID_ITEMS)
        }
    }

    enum class Status {
        CREATED,
    }

    companion object {
        fun create(
            userId: Long,
            idempotencyKey: IdempotencyKey,
            items: List<OrderItem>,
        ): Order = Order(
            id = null,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = Status.CREATED,
            items = items,
        )

        fun retrieve(
            id: Long,
            userId: Long,
            idempotencyKey: IdempotencyKey,
            status: Status,
            items: List<OrderItem>,
        ): Order = Order(
            id = id,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = status,
            items = items,
        )
    }
}
