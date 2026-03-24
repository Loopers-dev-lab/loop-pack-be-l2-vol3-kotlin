package com.loopers.domain.user.event

import java.time.Instant

data class UserActionEvent(
    val userId: Long,
    val actionType: ActionType,
    val targetId: Long,
    val timestamp: Instant = Instant.now(),
)

enum class ActionType {
    PRODUCT_VIEWED,
    PRODUCT_LIKED,
    PRODUCT_UNLIKED,
    ORDER_PLACED,
}
