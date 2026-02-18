package com.loopers.domain.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product

class OrderItemFactory {

    fun create(product: Product, brand: Brand, quantity: Int): OrderItem {
        product.assertOrderable(quantity)
        return OrderItem.create(
            product = product,
            brand = brand,
            quantity = quantity,
        )
    }
}
