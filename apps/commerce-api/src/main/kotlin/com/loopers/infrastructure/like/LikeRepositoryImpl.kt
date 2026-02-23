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
}
