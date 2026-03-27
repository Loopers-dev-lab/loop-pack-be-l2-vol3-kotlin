package com.loopers.application.useraction

import com.loopers.domain.useraction.UserActionTargetType
import com.loopers.domain.useraction.UserActionType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogUserAction(
    val action: UserActionType,
    val targetType: UserActionTargetType,
)
