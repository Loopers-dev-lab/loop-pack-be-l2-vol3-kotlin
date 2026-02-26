package com.loopers.application.like

import com.loopers.application.product.ProductService
import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
    private val productService: ProductService,
) {

    /**
     * 좋아요 등록
     * - 상품 존재 검증 (Soft Delete 포함)
     * - 기존 활성 좋아요 있음 → 기존 반환 (멱등성)
     * - Soft Delete 상태 → restore
     * - 없음 → 신규 생성
     */
    @Transactional
    fun addLike(userId: Long, productId: Long): LikeInfo {
        productService.validateProductExistsIncludingDeleted(productId)

        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)

        if (existingLike != null) {
            if (existingLike.isDeleted()) {
                existingLike.restore()
                return LikeInfo.from(likeRepository.save(existingLike))
            }
            return LikeInfo.from(existingLike)
        }

        return LikeInfo.from(likeRepository.save(Like(userId = userId, productId = productId)))
    }

    /**
     * 좋아요 취소
     * - 활성 좋아요 있음 → Soft Delete
     * - 없음 → no-op (멱등성)
     */
    @Transactional
    fun cancelLike(userId: Long, productId: Long) {
        val like = likeRepository.findActiveByUserIdAndProductId(userId, productId) ?: return

        like.delete()
        likeRepository.save(like)
    }

    @Transactional(readOnly = true)
    fun getUserLikes(userId: Long): List<LikeInfo> {
        return likeRepository.findAllActiveByUserId(userId).map { LikeInfo.from(it) }
    }
}
