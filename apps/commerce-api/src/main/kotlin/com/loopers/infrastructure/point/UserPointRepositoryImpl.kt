package com.loopers.infrastructure.point

import com.loopers.domain.point.model.UserPoint
import com.loopers.domain.point.repository.UserPointRepository
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.stereotype.Repository

interface UserPointJpaRepository : JpaRepository<UserPointEntity, Long> {
    fun findByRefUserId(refUserId: Long): UserPointEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByRefUserId(refUserId: Long): UserPointEntity?
}

@Repository
class UserPointRepositoryImpl(
    private val userPointJpaRepository: UserPointJpaRepository,
) : UserPointRepository {

    override fun save(userPoint: UserPoint): UserPoint {
        return userPointJpaRepository.save(UserPointEntity.fromDomain(userPoint)).toDomain()
    }

    override fun findById(id: Long): UserPoint? {
        return userPointJpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByUserId(userId: Long): UserPoint? {
        return userPointJpaRepository.findByRefUserId(userId)?.toDomain()
    }

    override fun findByUserIdForUpdate(userId: Long): UserPoint? {
        return userPointJpaRepository.findFirstByRefUserId(userId)?.toDomain()
    }
}
