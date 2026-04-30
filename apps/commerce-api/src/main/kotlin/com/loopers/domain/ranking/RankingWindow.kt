package com.loopers.domain.ranking

/**
 * 랭킹 시간 윈도우.
 *
 * API에서 어떤 시간 단위의 랭킹을 조회할지 지정한다.
 * DAILY/HOURLY는 Redis ZSET에서, WEEKLY/MONTHLY는 MV 테이블(DB)에서 조회한다.
 */
enum class RankingWindow {
    DAILY,
    HOURLY,
    WEEKLY,
    MONTHLY,
    ;

    fun isRedisBased(): Boolean = this == DAILY || this == HOURLY
    fun isMvBased(): Boolean = this == WEEKLY || this == MONTHLY

    companion object {
        fun from(value: String?): RankingWindow {
            if (value == null) return DAILY
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}
