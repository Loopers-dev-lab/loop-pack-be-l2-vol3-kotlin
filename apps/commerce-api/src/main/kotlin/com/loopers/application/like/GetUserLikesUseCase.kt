package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetUserLikesUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long): List<LikeWithProductInfo> {
        val likes = likeRepository.findAllByUserId(userId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.refProductId }
        val products = productRepository.findAllByIds(productIds).filter { it.isActive() }
        val productMap = products.associateBy { it.id }

        return likes.mapNotNull { like ->
            productMap[like.refProductId]?.let { product ->
                LikeWithProductInfo.from(like, product)
            }
        }
    }
}
