package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product

class OrderItem private constructor(
    val persistenceId: Long?,
    val productId: Long,
    val productName: String,
    val brandName: String,
    val price: Money,
    val quantity: Int,
) {

    fun getSubtotal(): Money {
        return price.multiply(quantity)
    }

    companion object {
        fun create(product: Product, brand: Brand, quantity: Int): OrderItem {
            require(quantity > 0) { "주문 수량은 0보다 커야 합니다." }
            return OrderItem(
                persistenceId = null,
                productId = requireNotNull(product.persistenceId) {
                    "저장되지 않은 상품으로는 주문 항목을 생성할 수 없습니다."
                },
                productName = product.name.value,
                brandName = brand.name.value,
                price = product.price,
                quantity = quantity,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            productId: Long,
            productName: String,
            brandName: String,
            price: Money,
            quantity: Int,
        ): OrderItem {
            return OrderItem(
                persistenceId = persistenceId,
                productId = productId,
                productName = productName,
                brandName = brandName,
                price = price,
                quantity = quantity,
            )
        }
    }
}
