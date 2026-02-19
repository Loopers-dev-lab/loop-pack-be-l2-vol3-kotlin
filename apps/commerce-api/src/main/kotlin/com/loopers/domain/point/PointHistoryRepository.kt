package com.loopers.domain.point

interface PointHistoryRepository {
    fun save(pointHistory: PointHistory): PointHistory
    fun findAllByUserPointId(userPointId: Long): List<PointHistory>
}
