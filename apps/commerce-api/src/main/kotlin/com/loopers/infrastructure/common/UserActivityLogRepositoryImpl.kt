package com.loopers.infrastructure.common

import com.loopers.domain.common.UserActivityLogModel
import com.loopers.domain.common.UserActivityLogRepository
import org.springframework.stereotype.Component

@Component
class UserActivityLogRepositoryImpl(
    private val userActivityLogJpaRepository: UserActivityLogJpaRepository,
) : UserActivityLogRepository {

    override fun save(log: UserActivityLogModel): UserActivityLogModel {
        return userActivityLogJpaRepository.save(log)
    }
}
