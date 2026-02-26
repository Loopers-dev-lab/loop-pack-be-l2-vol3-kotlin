package com.loopers.infrastructure.like

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface LikeJpaRepository : JpaRepository<LikeEntity, Long> {
    fun existsByRefUserIdAndRefProductId(refUserId: Long, refProductId: Long): Boolean
    fun findByRefUserIdAndRefProductId(refUserId: Long, refProductId: Long): LikeEntity?
    fun findAllByRefUserIdOrderByIdDesc(refUserId: Long): List<LikeEntity>
}

@Repository
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: Like): Like {
        return likeJpaRepository.save(LikeEntity.fromDomain(like)).toDomain()
    }

    override fun existsByUserIdAndProductId(userId: UserId, productId: ProductId): Boolean {
        return likeJpaRepository.existsByRefUserIdAndRefProductId(userId.value, productId.value)
    }

    override fun findByUserIdAndProductId(userId: UserId, productId: ProductId): Like? {
        return likeJpaRepository.findByRefUserIdAndRefProductId(userId.value, productId.value)?.toDomain()
    }

    override fun delete(like: Like) {
        likeJpaRepository.deleteById(like.id)
    }

    override fun findAllByUserId(userId: UserId): List<Like> {
        return likeJpaRepository.findAllByRefUserIdOrderByIdDesc(userId.value).map { it.toDomain() }
    }
}
