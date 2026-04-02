package com.loopers.support.annotation

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WaitingQueue(
    val name: String = "",
    val activeTokenTTLSeconds: Int = 300,
    val throughputPerServerPerSecond: Int = 175,
)
