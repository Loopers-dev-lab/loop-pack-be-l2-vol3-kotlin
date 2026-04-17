package com.loopers.application.ranking

enum class RankingPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ;

    companion object {
        fun fromOrNull(s: String?): RankingPeriod? =
            s?.let { input -> values().firstOrNull { v -> v.name.equals(input, ignoreCase = true) } }
    }
}
