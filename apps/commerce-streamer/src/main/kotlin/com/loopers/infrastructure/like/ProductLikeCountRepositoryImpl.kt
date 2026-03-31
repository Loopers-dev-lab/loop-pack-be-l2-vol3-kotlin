package com.loopers.infrastructure.like

import com.loopers.domain.metric.ProductLikeCountRepository
import org.springframework.stereotype.Repository

@Repository
class ProductLikeCountRepositoryImpl(
    private val productLikeJpaRepository: ProductLikeJpaRepository,
) : ProductLikeCountRepository {
    override fun countByProductId(productId: Long): Int =
        productLikeJpaRepository.countByProductId(productId).toInt()
}
