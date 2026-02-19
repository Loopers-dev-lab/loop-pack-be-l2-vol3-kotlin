package com.loopers.infrastructure.like

import com.loopers.domain.like.entity.Like
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.stereotype.Component

@Component
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: Like): Like {
        return likeJpaRepository.save(like)
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return likeJpaRepository.findByRefUserIdAndRefProductId(userId, productId)
    }

    override fun delete(like: Like) {
        likeJpaRepository.delete(like)
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return likeJpaRepository.findAllByRefUserIdOrderByIdDesc(userId)
    }
}
