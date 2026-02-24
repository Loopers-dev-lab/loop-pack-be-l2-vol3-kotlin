package com.loopers.domain.order.model

import com.loopers.domain.common.Money
import com.loopers.domain.common.annotation.AggregateRootOnly
import com.loopers.domain.order.OrderProductInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class OrderItem private constructor(
    refOrderId: Long,
    val refProductId: Long,
    val productName: String,
    val productPrice: Money,
    val quantity: Int,
) {

    val id: Long = 0

    var refOrderId: Long = refOrderId
        private set

    var status: ItemStatus = ItemStatus.ACTIVE
        private set

    init {
        if (quantity < 1) {
            throw CoreException(ErrorType.BAD_REQUEST, "수량은 1 이상이어야 합니다.")
        }
    }

    @AggregateRootOnly
    fun cancel() {
        if (status == ItemStatus.CANCELLED) {
            throw CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문 아이템입니다.")
        }
        status = ItemStatus.CANCELLED
    }

    enum class ItemStatus {
        ACTIVE,
        CANCELLED,
    }

    @AggregateRootOnly
    fun assignToOrder(orderId: Long) {
        this.refOrderId = orderId
    }

    companion object {
        fun create(product: OrderProductInfo, quantity: Int): OrderItem {
            return OrderItem(
                refOrderId = 0,
                refProductId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = quantity,
            )
        }

        fun fromPersistence(
            id: Long,
            refOrderId: Long,
            refProductId: Long,
            productName: String,
            productPrice: Money,
            quantity: Int,
            status: ItemStatus,
        ): OrderItem {
            return OrderItem(
                refOrderId = refOrderId,
                refProductId = refProductId,
                productName = productName,
                productPrice = productPrice,
                quantity = quantity,
            ).also { item ->
                OrderItem::class.java.getDeclaredField("id").apply {
                    isAccessible = true
                    set(item, id)
                }
                OrderItem::class.java.getDeclaredField("status").apply {
                    isAccessible = true
                    set(item, status)
                }
            }
        }
    }
}
