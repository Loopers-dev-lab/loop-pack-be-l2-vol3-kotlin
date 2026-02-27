package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class OrderDomainService {

    fun buildOrder(userId: Long, items: List<OrderItemCommand>): Order {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목이 비어있습니다.")
        }
        val order = Order(userId = userId)
        for (item in items) {
            order.addItem(
                productId = item.productId,
                productName = item.productName,
                brandName = item.brandName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
            )
        }
        return order
    }
}
