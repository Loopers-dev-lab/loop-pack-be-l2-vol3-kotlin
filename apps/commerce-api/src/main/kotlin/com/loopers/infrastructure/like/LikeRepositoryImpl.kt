package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import com.loopers.support.PageResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId)
    }

    override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean {
        return likeJpaRepository.existsByUserIdAndProductId(userId, productId)
    }

    override fun findActiveLikesByUserId(userId: Long, page: Int, size: Int): PageResult<Like> {
        val pageable = PageRequest.of(page, size)
        val result = likeJpaRepository.findActiveLikesByUserId(userId, pageable)

        return PageResult.of(
            content = result.content,
            page = page,
            size = size,
            totalElements = result.totalElements,
        )
    }

    override fun save(like: Like): Like {
        return likeJpaRepository.save(like)
    }

    override fun delete(like: Like) {
        likeJpaRepository.delete(like)
    }
}
