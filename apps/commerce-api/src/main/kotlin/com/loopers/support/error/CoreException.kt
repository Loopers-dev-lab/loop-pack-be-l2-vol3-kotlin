package com.loopers.support.error

class CoreException(
    val errorType: ErrorType,
    val customMessage: String? = null,
    val data: Any? = null,
) : RuntimeException(customMessage ?: errorType.message)
