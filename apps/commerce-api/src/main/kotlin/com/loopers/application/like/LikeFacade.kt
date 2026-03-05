package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.product.ProductService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeFacade(
    private val likeService: LikeService,
    private val productService: ProductService,
    private val likeTransactionExecutor: LikeTransactionExecutor,
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

    fun like(userId: Long, productId: Long) {
        try {
            likeTransactionExecutor.like(userId, productId)
        } catch (_: DataIntegrityViolationException) {
            // TOCTOU 경쟁 조건: 다른 스레드가 먼저 좋아요를 등록함 → 멱등 처리
        }
    }

    fun unlike(userId: Long, productId: Long) {
        likeTransactionExecutor.unlike(userId, productId)
    }
}
