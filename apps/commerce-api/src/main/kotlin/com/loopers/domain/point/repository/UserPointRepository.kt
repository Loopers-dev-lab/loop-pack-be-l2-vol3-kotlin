package com.loopers.domain.point.repository

import com.loopers.domain.point.model.UserPoint

interface UserPointRepository {
    fun save(userPoint: UserPoint): UserPoint
    fun findById(id: Long): UserPoint?
    fun findByUserId(userId: Long): UserPoint?
    fun findByUserIdForUpdate(userId: Long): UserPoint?
}
