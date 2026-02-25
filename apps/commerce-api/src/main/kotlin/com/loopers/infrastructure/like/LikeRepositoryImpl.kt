package com.loopers.infrastructure.like

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

    override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean {
        return likeJpaRepository.existsByRefUserIdAndRefProductId(userId, productId)
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return likeJpaRepository.findByRefUserIdAndRefProductId(userId, productId)?.toDomain()
    }

    override fun delete(like: Like) {
        likeJpaRepository.deleteById(like.id)
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return likeJpaRepository.findAllByRefUserIdOrderByIdDesc(userId).map { it.toDomain() }
    }
}
