package com.loopers.interfaces.api.ranking.dto

import com.loopers.domain.ranking.RankingPeriod
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

enum class RankingV1Period {
    DAILY,
    WEEKLY,
    MONTHLY,
    ;

    fun toDomain(): RankingPeriod = when (this) {
        DAILY -> RankingPeriod.DAILY
        WEEKLY -> RankingPeriod.WEEKLY
        MONTHLY -> RankingPeriod.MONTHLY
    }

    companion object {
        fun from(value: String): RankingV1Period =
            entries.find { it.name.lowercase() == value }
                ?: throw CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 period 값: $value")
    }
}
