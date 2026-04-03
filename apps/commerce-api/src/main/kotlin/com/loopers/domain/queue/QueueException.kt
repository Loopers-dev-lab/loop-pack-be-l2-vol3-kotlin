package com.loopers.domain.queue

import com.loopers.domain.DomainException

class QueueException(
    val error: QueueError,
    message: String,
) : DomainException(message)

enum class QueueError {
    ALREADY_IN_QUEUE,
    TOKEN_NOT_FOUND,
    TOKEN_EXPIRED,
}
