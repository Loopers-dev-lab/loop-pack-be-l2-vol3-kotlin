package com.loopers.domain.point.repository

import com.loopers.domain.point.model.PointHistory

interface PointHistoryRepository {
    fun save(pointHistory: PointHistory): PointHistory
    fun findAllByUserPointId(userPointId: Long): List<PointHistory>
}
