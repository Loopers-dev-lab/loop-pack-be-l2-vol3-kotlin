package com.loopers.infrastructure.like

import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

interface LikeJpaRepository : JpaRepository<LikeEntity, Long> {
    fun findByRefUserIdAndRefProductId(refUserId: Long, refProductId: Long): LikeEntity?
    fun findAllByRefUserIdOrderByIdDesc(refUserId: Long): List<LikeEntity>
}

@Repository
class LikeRepositoryImpl(
    private val jpa: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: Like): Like {
        return jpa.save(LikeEntity.fromDomain(like)).toDomain()
    }

    override fun findByUserIdAndProductId(userId: Long, productId: Long): Like? {
        return jpa.findByRefUserIdAndRefProductId(userId, productId)?.toDomain()
    }

    override fun delete(like: Like) {
        jpa.deleteById(like.id)
    }

    override fun findAllByUserId(userId: Long): List<Like> {
        return jpa.findAllByRefUserIdOrderByIdDesc(userId).map { it.toDomain() }
    }
}
