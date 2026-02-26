package com.loopers.domain.order.model

import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.annotation.AggregateRootOnly
import com.loopers.domain.common.vo.OrderId
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.order.OrderProductInfo
import com.loopers.domain.common.vo.Quantity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class OrderItem private constructor(
    val id: Long = 0,
    val refProductId: ProductId,
    val productName: String,
    val productPrice: Money,
    val quantity: Quantity,
    refOrderId: OrderId,
) {
    var refOrderId: OrderId = refOrderId
        private set

    var status: ItemStatus = ItemStatus.ACTIVE
        private set

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
    fun assignToOrder(orderId: OrderId) {
        this.refOrderId = orderId
    }

    companion object {
        fun create(product: OrderProductInfo, quantity: Quantity): OrderItem {
            return OrderItem(
                refOrderId = OrderId(0),
                refProductId = product.id,
                productName = product.name,
                productPrice = product.price,
                quantity = quantity,
            )
        }

        fun fromPersistence(
            id: Long,
            refOrderId: OrderId,
            refProductId: ProductId,
            productName: String,
            productPrice: Money,
            quantity: Quantity,
            status: ItemStatus,
        ): OrderItem {
            return OrderItem(
                id = id,
                refOrderId = refOrderId,
                refProductId = refProductId,
                productName = productName,
                productPrice = productPrice,
                quantity = quantity,
            ).also {
                it.status = status
            }
        }
    }
}
