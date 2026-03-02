package com.loopers.domain.point

import com.loopers.domain.point.model.PointHistory
import com.loopers.domain.point.repository.PointHistoryRepository

class FakePointHistoryRepository : PointHistoryRepository {

    private val histories = mutableListOf<PointHistory>()
    private var sequence = 1L

    override fun save(pointHistory: PointHistory): PointHistory {
        if (pointHistory.id != 0L) {
            histories.removeIf { it.id == pointHistory.id }
            histories.add(pointHistory)
        } else {
            setId(pointHistory, sequence++)
            histories.add(pointHistory)
        }
        return pointHistory
    }

    override fun findAllByUserPointId(userPointId: Long): List<PointHistory> {
        return histories.filter { it.refUserPointId == userPointId }
    }

    private fun setId(entity: PointHistory, id: Long) {
        PointHistory::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
