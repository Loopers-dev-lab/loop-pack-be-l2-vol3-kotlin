package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.springframework.stereotype.Repository

@Repository
class LikeRepositoryImpl(
    private val jpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun addLike(userId: Long, productId: Long): Boolean {
        return jpaRepository.insertIgnore(userId, productId) > 0
    }

    override fun removeLike(userId: Long, productId: Long): Int {
        return jpaRepository.deleteByUserIdAndProductId(userId, productId)
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return jpaRepository.findAllByUserId(userId)
            .map { LikeMapper.toDomain(it) }
    }
}
