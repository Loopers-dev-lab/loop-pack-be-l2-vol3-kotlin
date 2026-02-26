package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.stereotype.Component

@Component
class ProductLikeRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
) : ProductLikeRepository {
    override fun save(productLike: ProductLikeModel): ProductLikeModel {
        return productLikeJpaRepository.save(productLike)
    }

    override fun findByMemberIdAndProductId(memberId: Long, productId: Long): ProductLikeModel? {
        return productLikeJpaRepository.findByMemberIdAndProductId(memberId, productId)
    }

    override fun deleteByMemberIdAndProductId(memberId: Long, productId: Long) {
        productLikeJpaRepository.deleteByMemberIdAndProductId(memberId, productId)
    }

    override fun findAllByMemberId(memberId: Long): List<ProductLikeModel> {
        return productLikeJpaRepository.findAllByMemberId(memberId)
    }
}
