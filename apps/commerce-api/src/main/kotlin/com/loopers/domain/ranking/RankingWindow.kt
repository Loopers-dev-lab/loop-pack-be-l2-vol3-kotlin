package com.loopers.domain.ranking

/**
 * 랭킹 시간 윈도우.
 *
 * API에서 어떤 시간 단위의 랭킹을 조회할지 지정한다.
 */
enum class RankingWindow {
    DAILY,
    HOURLY,
    ;

    companion object {
        fun from(value: String?): RankingWindow {
            if (value == null) return DAILY
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}
