package com.loopers.infrastructure.point

import com.loopers.domain.point.entity.UserPoint
import com.loopers.domain.point.repository.UserPointRepository
import org.springframework.stereotype.Component

@Component
class UserPointRepositoryImpl(
    private val userPointJpaRepository: UserPointJpaRepository,
) : UserPointRepository {

    override fun save(userPoint: UserPoint): UserPoint {
        return userPointJpaRepository.save(userPoint)
    }

    override fun findById(id: Long): UserPoint? {
        return userPointJpaRepository.findById(id).orElse(null)
    }

    override fun findByUserId(userId: Long): UserPoint? {
        return userPointJpaRepository.findByRefUserId(userId)
    }

    override fun findByUserIdForUpdate(userId: Long): UserPoint? {
        return userPointJpaRepository.findFirstByRefUserId(userId)
    }
}
