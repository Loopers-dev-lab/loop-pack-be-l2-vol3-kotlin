package com.loopers.application.like

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetUserLikesUseCase(
    private val likeService: LikeService,
    private val catalogService: CatalogService,
) {
    @Transactional(readOnly = true)
    fun execute(userId: Long): List<LikeWithProductInfo> {
        val likes = likeService.getLikesByUserId(userId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.refProductId }
        val products = catalogService.getActiveProductsByIds(productIds)
        val productMap = products.associateBy { it.id }

        return likes.mapNotNull { like ->
            productMap[like.refProductId]?.let { product ->
                LikeWithProductInfo.from(like, product)
            }
        }
    }
}
