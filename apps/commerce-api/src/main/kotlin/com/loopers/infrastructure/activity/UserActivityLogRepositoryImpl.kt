package com.loopers.infrastructure.activity

import com.loopers.domain.activity.TargetType
import com.loopers.domain.activity.UserActivityLog
import com.loopers.domain.activity.UserActivityLogRepository
import org.springframework.stereotype.Component

@Component
class UserActivityLogRepositoryImpl(
    private val userActivityLogJpaRepository: UserActivityLogJpaRepository,
) : UserActivityLogRepository {

    override fun save(log: UserActivityLog): UserActivityLog {
        return userActivityLogJpaRepository.save(log)
    }

    override fun findAllByUserId(userId: Long): List<UserActivityLog> {
        return userActivityLogJpaRepository.findAllByUserId(userId)
    }

    override fun findAllByTargetTypeAndTargetId(targetType: TargetType, targetId: Long): List<UserActivityLog> {
        return userActivityLogJpaRepository.findAllByTargetTypeAndTargetId(targetType, targetId)
    }
}
