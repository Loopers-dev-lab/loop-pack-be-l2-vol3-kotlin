package com.loopers.infrastructure.ranking

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 랭킹 Redis 키 전략.
 *
 * 일간 키: ranking:all:{yyyyMMdd}
 * 시간 키: ranking:all:hourly:{yyyyMMddHH}
 *
 * 만료: 절대 시각 기반 (EXPIREAT)
 *  - 일간: 해당 날짜 + 2일 자정
 *  - 시간: 해당 시각 + 2시간의 정각
 *
 * sliding TTL을 쓰면 배치마다 만료 시각이 연장되어 의도보다 길게 살아남을 수 있다.
 * 절대 시각으로 고정하면 "이 키는 해당 시간 윈도우의 데이터이므로 N시간 뒤에 만료"가 명시적이다.
 */
object RankingRedisKeys {

    private const val DAILY_PREFIX = "ranking:all"
    private const val HOURLY_PREFIX = "ranking:all:hourly"
    private const val DAILY_TTL_DAYS: Long = 2L
    private const val HOURLY_TTL_HOURS: Long = 2L

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")
    private val HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH")

    fun dailyKey(dateKey: String): String = "$DAILY_PREFIX:$dateKey"

    fun hourlyKey(hourKey: String): String = "$HOURLY_PREFIX:$hourKey"

    /**
     * 일간 키의 만료 시각(Unix epoch second).
     * 예: dateKey="20260409" -> 20260411 00:00:00 (Asia/Seoul)
     */
    fun dailyExpireAtEpochSecond(dateKey: String): Long {
        return LocalDate.parse(dateKey, DATE_FORMAT)
            .plusDays(DAILY_TTL_DAYS)
            .atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond()
    }

    /**
     * 시간 키의 만료 시각(Unix epoch second).
     * 예: hourKey="2026040914" -> 2026040916 00:00 (2시간 뒤 정각)
     */
    fun hourlyExpireAtEpochSecond(hourKey: String): Long {
        return LocalDateTime.parse(hourKey, HOUR_FORMAT)
            .plusHours(HOURLY_TTL_HOURS)
            .atZone(ZoneId.systemDefault())
            .toEpochSecond()
    }

    /**
     * 현재 시각 기반으로 일간/시간 키의 문자열을 생성한다.
     */
    fun currentDateKey(now: LocalDateTime): String = now.format(DATE_FORMAT)

    fun currentHourKey(now: LocalDateTime): String = now.format(HOUR_FORMAT)
}
