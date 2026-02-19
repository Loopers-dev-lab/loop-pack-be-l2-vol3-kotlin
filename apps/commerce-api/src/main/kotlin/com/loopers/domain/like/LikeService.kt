package com.loopers.domain.like

import com.loopers.domain.like.entity.Like
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {

    @Transactional
    fun addLike(userId: Long, productId: Long): Boolean {
        val existing = likeRepository.findByUserIdAndProductId(userId, productId)
        if (existing != null) {
            return false
        }
        likeRepository.save(Like(refUserId = userId, refProductId = productId))
        return true
    }

    @Transactional
    fun removeLike(userId: Long, productId: Long): Boolean {
        val existing = likeRepository.findByUserIdAndProductId(userId, productId)
            ?: return false
        likeRepository.delete(existing)
        return true
    }

    @Transactional(readOnly = true)
    fun getLikesByUserId(userId: Long): List<Like> {
        return likeRepository.findAllByUserId(userId)
    }
}
