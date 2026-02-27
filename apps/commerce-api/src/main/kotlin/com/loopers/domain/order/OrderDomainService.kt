package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class OrderDomainService {

    fun placeOrder(command: CreateOrderCommand): Order {
        val orderItems = command.products.map { product ->
            val qty = command.quantities[product.id]
                ?: throw CoreException(ErrorType.BAD_REQUEST, "주문 수량 정보가 없습니다.")
            val brand = command.brands[product.brandId]
                ?: throw CoreException(ErrorType.BAD_REQUEST, "브랜드 정보가 없습니다.")
            OrderItem(
                productId = product.id,
                quantity = qty,
                productSnapshot = ProductSnapshot.from(product, brand),
                priceSnapshot = PriceSnapshot.from(product),
            )
        }

        val order = Order(userId = command.userId)
        orderItems.forEach { order.addItem(it) }
        order.calculateTotalAmount()
        return order
    }
}
