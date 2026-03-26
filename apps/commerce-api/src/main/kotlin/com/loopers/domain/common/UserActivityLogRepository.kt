package com.loopers.domain.common

interface UserActivityLogRepository {
    fun save(log: UserActivityLogModel): UserActivityLogModel
}
