package com.loopers.infrastructure.like

import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
    private val likeMapper: LikeMapper,
) : LikeRepository {

    override fun save(like: Like): Like {
        val entity = likeMapper.toEntity(like)
        try {
            val savedEntity = likeJpaRepository.save(entity)
            return likeMapper.toDomain(savedEntity)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.ALREADY_LIKED)
        }
    }

    override fun delete(like: Like) {
        likeJpaRepository.deleteById(requireNotNull(like.id))
    }

    override fun findById(id: Long): Like? {
        return likeJpaRepository.findById(id)
            .map { likeMapper.toDomain(it) }
            .orElse(null)
    }

    override fun findAllByMemberId(memberId: Long): List<Like> {
        return likeJpaRepository.findAllByMemberId(memberId).map { likeMapper.toDomain(it) }
    }

    override fun existsByMemberIdAndProductId(memberId: Long, productId: Long): Boolean {
        return likeJpaRepository.existsByMemberIdAndProductId(memberId, productId)
    }

    override fun countByProductId(productId: Long): Long {
        return likeJpaRepository.countByProductId(productId)
    }

    override fun countByProductIds(productIds: List<Long>): Map<Long, Long> {
        return likeJpaRepository.countByProductIdIn(productIds)
            .associate { (it[0] as Long) to (it[1] as Long) }
    }
}
