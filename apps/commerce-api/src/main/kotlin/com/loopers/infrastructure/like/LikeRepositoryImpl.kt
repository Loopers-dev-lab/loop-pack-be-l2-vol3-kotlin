package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import org.springframework.data.domain.PageRequest
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

    override fun findAllByUserId(userId: Long, page: Int, size: Int): List<Like> {
        return jpaRepository.findAllByUserId(userId, PageRequest.of(page, size))
            .map { LikeMapper.toDomain(it) }
            .content
    }

    override fun countByUserId(userId: Long): Long {
        return jpaRepository.countByUserId(userId)
    }
}
