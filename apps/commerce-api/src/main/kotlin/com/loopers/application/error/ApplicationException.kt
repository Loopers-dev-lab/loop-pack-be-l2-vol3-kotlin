package com.loopers.application.error

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType

class ApplicationException(
    val httpStatus: Int,
    val code: String,
    override val message: String,
) : RuntimeException(message) {
    companion object {
        fun from(e: CoreException): ApplicationException {
            val status = when (e.errorType) {
                ErrorType.BAD_REQUEST -> 400
                ErrorType.NOT_FOUND -> 404
                ErrorType.UNAUTHORIZED -> 401
                ErrorType.FORBIDDEN -> 403
                ErrorType.CONFLICT -> 409
                ErrorType.INTERNAL_ERROR -> 500
            }
            return ApplicationException(
                httpStatus = status,
                code = e.errorType.code,
                message = e.customMessage ?: e.errorType.message,
            )
        }
    }
}
