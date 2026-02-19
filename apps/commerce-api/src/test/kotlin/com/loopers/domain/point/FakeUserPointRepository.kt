package com.loopers.domain.point

import com.loopers.domain.BaseEntity
import com.loopers.domain.point.entity.UserPoint
import com.loopers.domain.point.repository.UserPointRepository

class FakeUserPointRepository : UserPointRepository {

    private val userPoints = mutableListOf<UserPoint>()
    private var sequence = 1L

    override fun save(userPoint: UserPoint): UserPoint {
        if (userPoint.id != 0L) {
            userPoints.removeIf { it.id == userPoint.id }
            userPoints.add(userPoint)
        } else {
            setEntityId(userPoint, sequence++)
            userPoints.add(userPoint)
        }
        return userPoint
    }

    override fun findById(id: Long): UserPoint? {
        return userPoints.find { it.id == id }
    }

    override fun findByUserId(userId: Long): UserPoint? {
        return userPoints.find { it.refUserId == userId }
    }

    private fun setEntityId(entity: BaseEntity, id: Long) {
        BaseEntity::class.java.getDeclaredField("id").apply {
            isAccessible = true
            set(entity, id)
        }
    }
}
