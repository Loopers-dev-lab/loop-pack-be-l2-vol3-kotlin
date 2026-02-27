package com.loopers.domain.like

interface LikeRepository {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike?
    fun save(productLike: ProductLike): ProductLike
    fun delete(productLike: ProductLike)
    fun findAllByUserId(userId: Long): List<ProductLike>
}
