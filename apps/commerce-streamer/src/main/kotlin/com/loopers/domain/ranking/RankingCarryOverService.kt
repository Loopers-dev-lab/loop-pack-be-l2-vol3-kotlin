package com.loopers.domain.ranking

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 랭킹 콜드 스타트 완화를 위한 Score Carry-Over 서비스.
 *
 * 전일 랭킹 점수에 감쇠 가중치를 곱해서 다음 날 키에 복사한다.
 * 새 날짜가 시작될 때 "아직 이벤트가 없는 상품"이 0점으로 시작하는 문제를 완화한다.
 *
 * 감쇠 가중치가 중요한 이유:
 * - 너무 크면 (e.g. 0.5): 어제의 인기 상품이 오늘도 계속 상위 고정 → 신선도 저하
 * - 너무 작으면 (e.g. 0.01): carry-over 효과가 미미
 * - 보통 0.1 내외 (전일 점수의 10% 반영) — 오늘 활동이 쌓이면 금세 오버라이트됨
 */
@Component
class RankingCarryOverService(
    private val rankingRepository: RankingRepository,
) {

    companion object {
        private val log = LoggerFactory.getLogger(RankingCarryOverService::class.java)
        private val DATE_KEY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")

        /**
         * 전일 점수의 몇 배를 복사할지 결정한다.
         * 낮게 잡아서 오늘 실제 활동이 금방 오버라이트하도록 유도한다.
         */
        const val CARRY_OVER_WEIGHT = 0.1
    }

    /**
     * 기준 날짜(today)의 랭킹을 다음 날(tomorrow) 키로 복사한다.
     *
     * 스케줄러 또는 Admin에서 호출 가능하다.
     *
     * @param today 기준 날짜 (yyyyMMdd)
     */
    fun carryOverToNextDay(today: LocalDate) {
        val fromDateKey = today.format(DATE_KEY_FORMAT)
        val toDateKey = today.plusDays(1).format(DATE_KEY_FORMAT)

        try {
            rankingRepository.carryOverScores(
                fromDateKey = fromDateKey,
                toDateKey = toDateKey,
                weight = CARRY_OVER_WEIGHT,
            )
            log.info(
                "[랭킹] Score Carry-Over 완료 [from={}, to={}, weight={}]",
                fromDateKey,
                toDateKey,
                CARRY_OVER_WEIGHT,
            )
        } catch (e: Exception) {
            log.error(
                "[랭킹] Score Carry-Over 실패 [from={}, to={}]",
                fromDateKey,
                toDateKey,
                e,
            )
        }
    }
}
