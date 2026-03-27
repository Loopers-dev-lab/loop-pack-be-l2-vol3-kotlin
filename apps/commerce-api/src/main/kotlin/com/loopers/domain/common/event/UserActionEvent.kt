package com.loopers.domain.common.event

import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType

data class UserActionEvent(
    val memberId: Long,
    val actionType: UserActionType,
    val targetType: UserActionTargetType,
    val targetId: Long,
)
