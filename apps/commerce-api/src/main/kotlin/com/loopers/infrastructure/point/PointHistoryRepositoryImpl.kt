package com.loopers.infrastructure.point

import com.loopers.domain.point.entity.PointHistory
import com.loopers.domain.point.repository.PointHistoryRepository
import org.springframework.stereotype.Component

@Component
class PointHistoryRepositoryImpl(
    private val pointHistoryJpaRepository: PointHistoryJpaRepository,
) : PointHistoryRepository {

    override fun save(pointHistory: PointHistory): PointHistory {
        return pointHistoryJpaRepository.save(pointHistory)
    }

    override fun findAllByUserPointId(userPointId: Long): List<PointHistory> {
        return pointHistoryJpaRepository.findAllByRefUserPointId(userPointId)
    }
}
