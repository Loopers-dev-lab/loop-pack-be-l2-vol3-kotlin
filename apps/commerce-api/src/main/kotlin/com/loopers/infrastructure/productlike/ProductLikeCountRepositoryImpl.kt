package com.loopers.infrastructure.productlike

import com.loopers.domain.productlike.ProductLikeCount
import com.loopers.domain.productlike.ProductLikeCountRepository
import org.springframework.stereotype.Repository

@Repository
class ProductLikeCountRepositoryImpl(
    private val jpaRepository: ProductLikeCountJpaRepository,
) : ProductLikeCountRepository {

    override fun findByProductId(productId: Long): ProductLikeCount? =
        jpaRepository.findByProductId(productId)

    override fun increment(productId: Long) {
        jpaRepository.increment(productId)
    }

    override fun decrement(productId: Long) {
        jpaRepository.decrement(productId)
    }

    override fun save(productLikeCount: ProductLikeCount): ProductLikeCount =
        jpaRepository.save(productLikeCount)

    override fun updateCount(productId: Long, count: Long) {
        jpaRepository.updateCount(productId, count)
    }
}
