package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Component

@Component
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: Like): Like {
        return likeJpaRepository.save(like)
    }

    override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean {
        return likeJpaRepository.existsByUserIdAndProductId(userId, productId)
    }

    override fun deleteByUserIdAndProductId(userId: Long, productId: Long): Boolean {
        val like = likeJpaRepository.findByUserIdAndProductId(userId, productId) ?: return false
        likeJpaRepository.delete(like)
        return true
    }

    override fun findProductIdsByUserId(userId: Long): List<Long> {
        return likeJpaRepository.findByUserId(userId).map { it.productId }
    }
}
