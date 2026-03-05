package com.loopers.application.like

import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository

class FakeProductLikeRepository : ProductLikeRepository {
    private val store = mutableListOf<ProductLikeModel>()

    override fun save(productLike: ProductLikeModel): ProductLikeModel {
        store.add(productLike)
        return productLike
    }

    override fun findByMemberIdAndProductId(memberId: Long, productId: Long): ProductLikeModel? {
        return store.find { it.memberId == memberId && it.productId == productId }
    }

    override fun deleteByMemberIdAndProductId(memberId: Long, productId: Long) {
        store.removeAll { it.memberId == memberId && it.productId == productId }
    }

    override fun findAllByMemberId(memberId: Long): List<ProductLikeModel> {
        return store.filter { it.memberId == memberId }
    }
}
