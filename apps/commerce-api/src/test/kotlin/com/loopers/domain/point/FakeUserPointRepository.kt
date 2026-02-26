package com.loopers.domain.point

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.point.model.UserPoint
import com.loopers.domain.point.repository.UserPointRepository

class FakeUserPointRepository : UserPointRepository {

    private val userPoints = mutableListOf<UserPoint>()
    private var sequence = 1L

    override fun save(userPoint: UserPoint): UserPoint {
        if (userPoint.id != 0L) {
            userPoints.removeIf { it.id == userPoint.id }
            userPoints.add(userPoint)
        } else {
            setField(userPoint, "id", sequence++)
            userPoints.add(userPoint)
        }
        return userPoint
    }

    override fun findById(id: Long): UserPoint? {
        return userPoints.find { it.id == id }
    }

    override fun findByUserId(userId: UserId): UserPoint? {
        return userPoints.find { it.refUserId == userId }
    }

    override fun findByUserIdForUpdate(userId: UserId): UserPoint? {
        return findByUserId(userId)
    }

    private fun setField(target: Any, fieldName: String, value: Any) {
        UserPoint::class.java.getDeclaredField(fieldName).apply {
            isAccessible = true
            set(target, value)
        }
    }
}
