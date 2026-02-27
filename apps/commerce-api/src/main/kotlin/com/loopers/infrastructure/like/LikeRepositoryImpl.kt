package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: LikeModel): LikeModel {
        return likeJpaRepository.save(like)
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): LikeModel? {
        return likeJpaRepository.findByUserIdAndProductId(userId, productId)
    }

    override fun findAllByUserIdAndDeletedAtIsNull(userId: Long, pageable: Pageable): Page<LikeModel> {
        return likeJpaRepository.findAllByUserIdAndDeletedAtIsNull(userId, pageable)
    }
}
