package com.loopers.support.validator

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

object PageValidator {
    private val VALID_SIZES = listOf(20, 50, 100)

    fun validatePageRequest(page: Int, size: Int) {
        if (size !in VALID_SIZES) {
            throw CoreException(ErrorType.BAD_REQUEST, "size는 20, 50, 100만 가능합니다")
        }

        if (page < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "page는 음수일 수 없습니다")
        }
    }
}
