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

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId)
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return likeJpaRepository.findAllByUserId(userId)
    }

    override fun delete(like: Like) {
        likeJpaRepository.delete(like)
    }

    override fun deleteAllByProductId(productId: Long) {
        likeJpaRepository.deleteAllByProductId(productId)
    }
}
