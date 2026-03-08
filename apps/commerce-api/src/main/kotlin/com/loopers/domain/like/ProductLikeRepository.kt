package com.loopers.domain.like

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface ProductLikeRepository {
    fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLikeModel?
    fun findAllByUserId(userId: Long, pageable: Pageable): Slice<ProductLikeModel>
    fun save(like: ProductLikeModel): ProductLikeModel
    fun delete(like: ProductLikeModel)
}
