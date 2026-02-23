package com.loopers.application.like

import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.like.LikeService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddLikeUseCase(
    private val likeService: LikeService,
    private val catalogService: CatalogService,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        catalogService.getActiveProduct(productId)
        val added = likeService.addLike(userId, productId)
        if (added) catalogService.increaseLikeCount(productId)
    }
}
