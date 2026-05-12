package com.loopers.batch.job.ranking

enum class FixedRankingScope {
    WEEK_FIXED,
    MONTH_FIXED,
    ALL,
    ;

    companion object {
        fun from(raw: String?): FixedRankingScope =
            runCatching { valueOf(raw?.replace('-', '_')?.uppercase() ?: ALL.name) }
                .getOrElse {
                    throw IllegalArgumentException("scope는 week-fixed, month-fixed, all 중 하나여야 합니다.")
                }
    }
}
