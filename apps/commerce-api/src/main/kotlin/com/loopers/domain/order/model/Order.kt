package com.loopers.domain.order.model

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import java.time.ZonedDateTime

class Order private constructor(
    refUserId: Long,
    status: OrderStatus,
    totalPrice: Money,
    val createdAt: ZonedDateTime = ZonedDateTime.now(),
    val updatedAt: ZonedDateTime = ZonedDateTime.now(),
    val deletedAt: ZonedDateTime? = null,
) {

    val id: Long = 0

    var refUserId: Long = refUserId
        private set

    var status: OrderStatus = status
        private set

    var totalPrice: Money = totalPrice
        private set

    enum class OrderStatus {
        CREATED,
        PAID,
        CANCELLED,
        FAILED,
    }

    fun cancelItem(item: OrderItem) {
        if (item.status == OrderItem.ItemStatus.CANCELLED) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문 아이템입니다.")
        }
        item.cancel()
        totalPrice = totalPrice - (item.productPrice * item.quantity)
    }

    companion object {
        fun create(userId: Long, totalPrice: Money): Order {
            return Order(
                refUserId = userId,
                status = OrderStatus.CREATED,
                totalPrice = totalPrice,
            )
        }

        fun fromPersistence(
            id: Long,
            refUserId: Long,
            status: OrderStatus,
            totalPrice: Money,
            createdAt: ZonedDateTime,
            updatedAt: ZonedDateTime,
            deletedAt: ZonedDateTime?,
        ): Order {
            return Order(
                refUserId = refUserId,
                status = status,
                totalPrice = totalPrice,
                createdAt = createdAt,
                updatedAt = updatedAt,
                deletedAt = deletedAt,
            ).also {
                Order::class.java.getDeclaredField("id").apply {
                    isAccessible = true
                    set(it, id)
                }
            }
        }
    }
}
