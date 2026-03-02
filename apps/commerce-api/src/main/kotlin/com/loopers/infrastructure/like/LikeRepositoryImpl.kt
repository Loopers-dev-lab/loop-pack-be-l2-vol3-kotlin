package com.loopers.infrastructure.like

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.like.model.Like
import com.loopers.domain.like.repository.LikeRepository
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface LikeJpaRepository : JpaRepository<LikeEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByRefUserIdAndRefProductId(refUserId: Long, refProductId: Long): LikeEntity?

    fun findAllByRefUserIdOrderByIdDesc(refUserId: Long): List<LikeEntity>
}

@Repository
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {

    override fun save(like: Like): Like {
        return likeJpaRepository.save(LikeEntity.fromDomain(like)).toDomain()
    }

    override fun findByUserIdAndProductIdForUpdate(userId: UserId, productId: ProductId): Like? {
        return likeJpaRepository.findForUpdateByRefUserIdAndRefProductId(userId.value, productId.value)?.toDomain()
    }

    override fun delete(like: Like) {
        likeJpaRepository.deleteById(like.id)
    }

    override fun findAllByUserId(userId: UserId): List<Like> {
        return likeJpaRepository.findAllByRefUserIdOrderByIdDesc(userId.value).map { it.toDomain() }
    }
}
