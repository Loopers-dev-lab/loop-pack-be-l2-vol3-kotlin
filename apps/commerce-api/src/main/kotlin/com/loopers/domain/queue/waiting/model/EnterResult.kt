package com.loopers.domain.queue.waiting.model

sealed interface EnterResult {
    data class Entered(val position: Long) : EnterResult

    object AlreadyHasToken : EnterResult

    object QueueFull : EnterResult
}
