package com.loopers.infrastructure.point

import com.loopers.domain.point.entity.UserPoint
import org.springframework.data.jpa.repository.JpaRepository

interface UserPointJpaRepository : JpaRepository<UserPoint, Long> {
    fun findByRefUserId(refUserId: Long): UserPoint?
}
