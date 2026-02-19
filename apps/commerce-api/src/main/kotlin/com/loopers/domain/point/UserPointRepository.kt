package com.loopers.domain.point

interface UserPointRepository {
    fun save(userPoint: UserPoint): UserPoint
    fun findById(id: Long): UserPoint?
    fun findByUserId(userId: Long): UserPoint?
}
