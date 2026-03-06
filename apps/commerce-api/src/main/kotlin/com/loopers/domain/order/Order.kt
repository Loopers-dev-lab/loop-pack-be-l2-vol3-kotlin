package com.loopers.domain.order

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class Order private constructor(
    val id: Long?,
    val userId: Long,
    val idempotencyKey: IdempotencyKey,
    val status: Status,
    val items: List<OrderItem>,
    val issuedCouponId: Long?,
    val discountAmount: Money,
    val createdAt: ZonedDateTime?,
) {
    init {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.ORDER_INVALID_ITEMS)
        }
    }

    fun totalAmount(): Money =
        items
            .map { it.snapshot.sellingPrice.multiply(it.quantity) }
            .reduce { acc, money -> acc + money }

    fun finalAmount(): Money {
        val total = totalAmount()
        return if (discountAmount.isGreaterThanOrEqual(total)) Money.ZERO else total - discountAmount
    }

    enum class Status {
        CREATED,
    }

    companion object {
        fun create(
            userId: Long,
            idempotencyKey: IdempotencyKey,
            items: List<OrderItem>,
            issuedCouponId: Long? = null,
            discountAmount: Money = Money.ZERO,
        ): Order = Order(
            id = null,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = Status.CREATED,
            items = items,
            issuedCouponId = issuedCouponId,
            discountAmount = discountAmount,
            createdAt = null,
        )

        fun retrieve(
            id: Long,
            userId: Long,
            idempotencyKey: IdempotencyKey,
            status: Status,
            items: List<OrderItem>,
            issuedCouponId: Long? = null,
            discountAmount: Money = Money.ZERO,
            createdAt: ZonedDateTime,
        ): Order = Order(
            id = id,
            userId = userId,
            idempotencyKey = idempotencyKey,
            status = status,
            items = items,
            issuedCouponId = issuedCouponId,
            discountAmount = discountAmount,
            createdAt = createdAt,
        )
    }
}
