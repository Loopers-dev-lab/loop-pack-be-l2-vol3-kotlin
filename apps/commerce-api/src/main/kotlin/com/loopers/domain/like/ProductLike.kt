package com.loopers.domain.like

class ProductLike private constructor(
    val id: Long?,
    val userId: Long,
    val productId: Long,
) {
    companion object {
        fun register(userId: Long, productId: Long): ProductLike {
            require(userId > 0) { "userId must be positive" }
            require(productId > 0) { "productId must be positive" }
            return ProductLike(id = null, userId = userId, productId = productId)
        }

        fun retrieve(id: Long, userId: Long, productId: Long): ProductLike =
            ProductLike(id = id, userId = userId, productId = productId)
    }
}
