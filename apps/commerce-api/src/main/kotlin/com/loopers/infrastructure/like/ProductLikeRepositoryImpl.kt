package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLikeModel
import com.loopers.domain.like.ProductLikeRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Component

@Component
class ProductLikeRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
) : ProductLikeRepository {
    override fun findByUserIdAndProductId(userId: Long, productId: Long): ProductLikeModel? {
        return productLikeJpaRepository.findByUserIdAndProductId(userId, productId)
    }

    override fun findAllByUserId(userId: Long, pageable: Pageable): Slice<ProductLikeModel> {
        return productLikeJpaRepository.findAllByUserId(userId, pageable)
    }

    override fun save(like: ProductLikeModel): ProductLikeModel {
        return productLikeJpaRepository.save(like)
    }

    override fun delete(like: ProductLikeModel) {
        productLikeJpaRepository.delete(like)
    }
}
