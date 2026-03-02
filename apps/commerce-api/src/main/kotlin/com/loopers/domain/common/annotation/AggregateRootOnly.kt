package com.loopers.domain.common.annotation

@RequiresOptIn(
    message = "이 메서드는 Aggregate Root를 통해서만 호출해야 합니다.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class AggregateRootOnly
