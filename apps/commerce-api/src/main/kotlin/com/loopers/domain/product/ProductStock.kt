package com.loopers.domain.product

import com.loopers.domain.common.Quantity

class ProductStock private constructor(
    val id: Long?,
    val productId: Long,
    val quantity: Quantity,
) {
    fun decrease(amount: Quantity): ProductStock =
        ProductStock(id = id, productId = productId, quantity = quantity.decrease(amount))

    fun increase(amount: Quantity): ProductStock =
        ProductStock(id = id, productId = productId, quantity = quantity.increase(amount))

    companion object {
        fun create(productId: Long, initialQuantity: Quantity): ProductStock =
            ProductStock(id = null, productId = productId, quantity = initialQuantity)

        fun retrieve(id: Long, productId: Long, quantity: Quantity): ProductStock =
            ProductStock(id = id, productId = productId, quantity = quantity)
    }
}
