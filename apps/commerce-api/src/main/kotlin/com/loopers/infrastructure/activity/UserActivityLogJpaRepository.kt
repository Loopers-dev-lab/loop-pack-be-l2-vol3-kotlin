package com.loopers.infrastructure.activity

import com.loopers.domain.activity.TargetType
import com.loopers.domain.activity.UserActivityLog
import org.springframework.data.jpa.repository.JpaRepository

interface UserActivityLogJpaRepository : JpaRepository<UserActivityLog, Long> {
    fun findAllByUserId(userId: Long): List<UserActivityLog>
    fun findAllByTargetTypeAndTargetId(targetType: TargetType, targetId: Long): List<UserActivityLog>
}
