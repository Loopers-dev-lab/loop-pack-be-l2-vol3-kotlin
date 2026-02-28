package com.loopers.application.like

import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 교차 집계: Like + Product(좋아요 수 감소)
 * MSA 분리 시 Product 좋아요 수 갱신 → 이벤트 기반 비동기 처리로 전환
 */
@Component
class RemoveLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun remove(userId: Long, productId: Long) {
        val affected = likeRepository.removeLike(userId, productId)
        if (affected > 0) {
            productRepository.decrementLikeCount(productId)
        }
    }
}
