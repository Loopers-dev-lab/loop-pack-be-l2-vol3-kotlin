package com.loopers.domain.order

import com.loopers.domain.DomainException

class OrderException(
    val error: OrderError,
    message: String,
) : DomainException(message)

enum class OrderError {
    NOT_CANCELLABLE,
    NOT_COMPLETABLE,
    NOT_OWNED,
}
