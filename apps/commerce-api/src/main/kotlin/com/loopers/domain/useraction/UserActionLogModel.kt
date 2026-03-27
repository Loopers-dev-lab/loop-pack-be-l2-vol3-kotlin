package com.loopers.domain.useraction

import java.time.ZonedDateTime

data class UserActionLogModel(
    val id: Long = 0,
    val memberId: Long,
    val actionType: UserActionType,
    val targetType: UserActionTargetType,
    val targetId: Long,
    val createdAt: ZonedDateTime? = null,
)
