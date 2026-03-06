package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {
    override fun save(like: LikeModel): LikeModel {
        return likeJpaRepository.save(like)
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel? {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId)
    }

    override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean {
        return likeJpaRepository.existsByUserIdAndProductId(userId, productId)
    }

    override fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int {
        return likeJpaRepository.deleteByUserIdAndProductId(userId, productId)
    }

    override fun findAllByUserId(
        userId: Long,
        pageable: Pageable,
    ): List<LikeModel> {
        return likeJpaRepository.findAllByUserId(userId, pageable)
    }
}
