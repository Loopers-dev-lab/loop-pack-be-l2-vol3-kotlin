package com.loopers.domain.event

import java.time.LocalDateTime

data class UserActionEvent(
    val userId: Long,
    val actionType: ActionType,
    val targetType: TargetType,
    val targetId: Long,
    val metadata: Map<String, Any> = emptyMap(),
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
