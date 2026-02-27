package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
) {

    @Transactional(readOnly = true)
    fun getUserLikes(authenticatedUserId: Long, userId: Long): List<LikeInfo> {
        if (authenticatedUserId != userId) {
            throw CoreException(ErrorType.FORBIDDEN, "타 유저의 정보에 접근할 수 없습니다")
        }
        val productIds = likeService.getLikedProductIds(userId)
        if (productIds.isEmpty()) return emptyList()
        val products = productService.getProductsByIds(productIds)
        return products.map { LikeInfo.from(it) }
    }

    @Transactional
    fun like(userId: Long, productId: Long) {
        val product = productService.getProduct(productId)
        val isNewLike = likeService.like(userId, productId)
        if (isNewLike) {
            productService.increaseLikeCount(product)
        }
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        val product = productService.getProduct(productId)
        val isDeleted = likeService.unlike(userId, productId)
        if (isDeleted) {
            productService.decreaseLikeCount(product)
        }
    }
}
