package com.loopers.domain.like

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class LikeService(
    private val likeRepository: LikeRepository,
) {

    @Transactional
    fun like(userId: Long, productId: Long): Pair<Boolean, LikeModel> {
        val existingLike = likeRepository.findByUserIdAndProductId(userId, productId)
        if (existingLike != null) return Pair(false, existingLike)

        return try {
            val newLike = likeRepository.save(LikeModel(userId = userId, productId = productId))
            Pair(true, newLike)
        } catch (e: DataIntegrityViolationException) {
            val saved = likeRepository.findByUserIdAndProductId(userId, productId)!!
            Pair(false, saved)
        }
    }

    @Transactional(readOnly = true)
    fun getLikedProducts(userId: Long, pageable: Pageable): List<LikeModel> {
        return likeRepository.findAllByUserId(userId, pageable)
    }

    @Transactional
    fun unlike(userId: Long, productId: Long): Boolean {
        return likeRepository.deleteByUserIdAndProductId(userId, productId) > 0
    }
}
