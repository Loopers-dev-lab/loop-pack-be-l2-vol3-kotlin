package com.loopers.infrastructure.point

import com.loopers.domain.point.entity.UserPoint
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

interface UserPointJpaRepository : JpaRepository<UserPoint, Long> {
    fun findByRefUserId(refUserId: Long): UserPoint?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findFirstByRefUserId(refUserId: Long): UserPoint?
}
