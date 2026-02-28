package com.loopers.domain.brand

import com.loopers.domain.DomainException

class BrandException(
    val error: BrandError,
    message: String,
) : DomainException(message)

enum class BrandError {
    DELETED,
}
