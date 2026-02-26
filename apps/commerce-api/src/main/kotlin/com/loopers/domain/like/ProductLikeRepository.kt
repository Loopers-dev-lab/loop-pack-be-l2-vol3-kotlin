package com.loopers.domain.like

interface ProductLikeRepository {
    fun save(productLike: ProductLikeModel): ProductLikeModel

    fun findByMemberIdAndProductId(memberId: Long, productId: Long): ProductLikeModel?

    fun deleteByMemberIdAndProductId(memberId: Long, productId: Long)

    fun findAllByMemberId(memberId: Long): List<ProductLikeModel>
}
