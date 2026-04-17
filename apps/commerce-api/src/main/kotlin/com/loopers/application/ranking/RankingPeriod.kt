package com.loopers.application.ranking

import com.loopers.support.error.CommonErrorCode
import com.loopers.support.error.CoreException

enum class RankingPeriod(val code: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    ;

    companion object {
        fun from(code: String?): RankingPeriod {
            if (code.isNullOrBlank()) return DAILY
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: throw CoreException(CommonErrorCode.INVALID_INPUT_VALUE)
        }
    }
}
