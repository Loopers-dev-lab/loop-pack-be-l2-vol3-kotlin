package com.loopers.domain.productlike

interface ProductLikeCountRepository {

    fun findByProductId(productId: Long): ProductLikeCount?

    fun increment(productId: Long)

    fun decrement(productId: Long)

    fun save(productLikeCount: ProductLikeCount): ProductLikeCount
}
