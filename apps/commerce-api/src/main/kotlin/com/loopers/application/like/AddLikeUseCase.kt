package com.loopers.application.like

import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 교차 집계: Like + Product(좋아요 수 증가)
 * MSA 분리 시 Product 좋아요 수 갱신 → 이벤트 기반 비동기 처리로 전환
 */
@Component
class AddLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun add(userId: Long, productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $productId")

        if (product.isDeleted()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다: $productId")
        }

        val inserted = likeRepository.addLike(userId, productId)
        if (inserted) {
            productRepository.incrementLikeCount(productId)
        }
    }
}
