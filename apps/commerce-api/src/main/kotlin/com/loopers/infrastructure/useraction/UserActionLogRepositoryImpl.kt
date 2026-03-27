package com.loopers.infrastructure.useraction

import com.loopers.domain.useraction.UserActionLogModel
import com.loopers.domain.useraction.UserActionLogRepository
import org.springframework.stereotype.Component

@Component
class UserActionLogRepositoryImpl(
    private val userActionLogJpaRepository: UserActionLogJpaRepository,
) : UserActionLogRepository {
    override fun save(log: UserActionLogModel): UserActionLogModel {
        return userActionLogJpaRepository.save(UserActionLogJpaModel.from(log)).toModel()
    }
}
