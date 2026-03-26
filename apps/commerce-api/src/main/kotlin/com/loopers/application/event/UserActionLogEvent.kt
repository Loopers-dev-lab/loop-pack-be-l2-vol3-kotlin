package com.loopers.application.event

import java.time.ZonedDateTime

data class UserActionLogEvent(
    val actionType: UserActionType,
    val memberId: Long?,
    val targetType: String,
    val targetId: String,
    val details: Map<String, Any?> = emptyMap(),
    val occurredAt: ZonedDateTime = ZonedDateTime.now(),
)
