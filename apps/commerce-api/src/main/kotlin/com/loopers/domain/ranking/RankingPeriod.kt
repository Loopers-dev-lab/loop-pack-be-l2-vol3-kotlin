package com.loopers.domain.ranking

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

enum class RankingPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ;

    companion object {
        fun from(value: String): RankingPeriod {
            return entries.find { it.name.lowercase() == value }
                ?: throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 period 값입니다: $value")
        }
    }
}
