package com.loopers.application.like

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val catalogService: CatalogService,
) {

    @Transactional
    fun addLike(userId: Long, productId: Long) {
        catalogService.getActiveProduct(productId)
        val added = likeService.addLike(userId, productId)
        if (added) {
            catalogService.increaseLikeCount(productId)
        }
    }

    @Transactional
    fun removeLike(userId: Long, productId: Long) {
        val product = catalogService.getProduct(productId)
        val removed = likeService.removeLike(userId, productId)
        if (removed && product.deletedAt == null) {
            catalogService.decreaseLikeCount(productId)
        }
    }

    @Transactional(readOnly = true)
    fun getLikes(userId: Long): List<LikeInfo> {
        val likes = likeService.getLikesByUserId(userId)
        if (likes.isEmpty()) return emptyList()

        val productIds = likes.map { it.refProductId }
        val products = catalogService.getActiveProductsByIds(productIds)
        val productMap = products.associateBy { it.id }

        return likes.mapNotNull { like ->
            productMap[like.refProductId]?.let { product ->
                LikeInfo.from(like, product)
            }
        }
    }
}
