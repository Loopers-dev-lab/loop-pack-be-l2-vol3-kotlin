package com.loopers.domain.like

data class ProductLikeInfo(
    val id: Long,
    val userId: Long,
    val productId: Long,
) {
    companion object {
        fun from(model: ProductLikeModel): ProductLikeInfo {
            return ProductLikeInfo(
                id = model.id,
                userId = model.userId,
                productId = model.productId,
            )
        }
    }
}
