package com.loopers.common

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 프로젝트 전체의 "오늘/어제/내일"을 KST(Asia/Seoul) 기준으로 통일한다.
 *
 * JVM 기본 타임존은 배포 환경마다 다를 수 있어 `LocalDate.now()`를 직접 쓰면
 * Redis 키(rank 일자), 배치 파라미터, carry-over 대상일이 틀어질 수 있다.
 * 이 프로젝트의 날짜 경계는 KST 하나로 고정하므로 모든 코드는 이 유틸만 사용한다.
 */
object DateUtils {

    val KST: ZoneId = ZoneId.of("Asia/Seoul")

    private val YYYYMMDD: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    fun todayKst(): LocalDate = LocalDate.now(KST)

    fun yesterdayKst(): LocalDate = todayKst().minusDays(1)

    fun tomorrowKst(): LocalDate = todayKst().plusDays(1)

    fun formatDate(date: LocalDate): String = date.format(YYYYMMDD)

    fun parseDate(text: String): LocalDate = LocalDate.parse(text, YYYYMMDD)
}
