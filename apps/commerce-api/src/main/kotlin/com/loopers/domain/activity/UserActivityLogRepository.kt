package com.loopers.domain.activity

interface UserActivityLogRepository {
    fun save(log: UserActivityLog): UserActivityLog
    fun findAllByUserId(userId: Long): List<UserActivityLog>
    fun findAllByTargetTypeAndTargetId(targetType: TargetType, targetId: Long): List<UserActivityLog>
}
