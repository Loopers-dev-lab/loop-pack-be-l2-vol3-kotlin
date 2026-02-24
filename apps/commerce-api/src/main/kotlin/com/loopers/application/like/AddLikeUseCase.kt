package com.loopers.application.like

import com.loopers.domain.catalog.product.repository.ProductRepository
import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddLikeUseCase(
    private val likeRepository: LikeRepository,
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun execute(userId: Long, productId: Long) {
        val product = productRepository.findById(productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        if (!product.isActive()) {
            throw CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다.")
        }

        val existing = likeRepository.findByUserIdAndProductId(userId, productId)
        if (existing != null) return

        val added = try {
            likeRepository.save(Like(refUserId = userId, refProductId = productId))
            true
        } catch (e: DataIntegrityViolationException) {
            // 동시 요청으로 DB unique constraint 위반 시 멱등 처리
            false
        }

        if (added) {
            productRepository.increaseLikeCount(productId)
        }
    }
}
