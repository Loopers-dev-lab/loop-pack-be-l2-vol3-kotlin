package com.loopers.domain.like

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {

    @Transactional
    fun like(userId: Long, productId: Long): LikeInfo {
        likeRepository.findByUserIdAndProductId(userId, productId)?.let {
            throw CoreException(ErrorType.CONFLICT, "이미 좋아요한 상품입니다.")
        }
        val like = Like(userId = userId, productId = productId)
        val saved = likeRepository.save(like)
        return LikeInfo.from(saved)
    }

    @Transactional
    fun unlike(userId: Long, productId: Long) {
        val like = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "좋아요하지 않은 상품입니다.")
        likeRepository.delete(like)
    }

    @Transactional(readOnly = true)
    fun findAllByUserId(userId: Long): List<LikeInfo> {
        return likeRepository.findAllByUserId(userId)
            .map { LikeInfo.from(it) }
    }

    @Transactional
    fun deleteAllByProductId(productId: Long) {
        likeRepository.deleteAllByProductId(productId)
    }
}
