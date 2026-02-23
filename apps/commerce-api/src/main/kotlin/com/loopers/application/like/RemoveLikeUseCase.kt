package com.loopers.application.like

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RemoveLikeUseCase(
    private val likeService: LikeService,
    private val catalogService: CatalogService,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        catalogService.getProduct(productId)
        val removed = likeService.removeLike(userId, productId)
        if (removed) catalogService.decreaseLikeCount(productId)
    }
}
