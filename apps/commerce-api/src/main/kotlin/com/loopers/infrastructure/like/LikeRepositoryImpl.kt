package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeRepository
import com.loopers.domain.like.ProductLike
import org.springframework.stereotype.Component

@Component
class LikeRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
) : LikeRepository {
    override fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLike? {
        return productLikeJpaRepository.findByUserIdAndProductId(userId, productId)
    }

    override fun save(productLike: ProductLike): ProductLike {
        return productLikeJpaRepository.save(productLike)
    }

    override fun delete(productLike: ProductLike) {
        productLikeJpaRepository.delete(productLike)
    }

    override fun findAllByUserId(userId: Long): List<ProductLike> {
        return productLikeJpaRepository.findAllByUserId(userId)
    }
}
