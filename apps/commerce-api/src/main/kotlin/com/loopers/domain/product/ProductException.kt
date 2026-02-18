package com.loopers.domain.product

import com.loopers.domain.DomainException

class ProductException(
    val error: ProductError,
    message: String,
) : DomainException(message)

enum class ProductError {
    DELETED,
    INSUFFICIENT_STOCK,
}
