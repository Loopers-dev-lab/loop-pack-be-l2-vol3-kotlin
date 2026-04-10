package com.loopers.interfaces.scheduler

import com.loopers.domain.ranking.RankingCarryOverService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 랭킹 Score Carry-Over 스케줄러.
 *
 * 매일 23:50에 실행되어 오늘의 랭킹 점수를 내일 키로 감쇠 복사한다.
 * 0시 직후 랭킹판이 비어있는 콜드 스타트 문제를 완화한다.
 *
 * 실행 시각을 0시 직전으로 잡은 이유:
 * - 오늘 이벤트를 최대한 많이 반영한 상태에서 복사하기 위함
 * - 00:00 직전이므로 TTL 경계(자정) 바로 직전에 동작
 */
@Component
class RankingCarryOverScheduler(
    private val rankingCarryOverService: RankingCarryOverService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(RankingCarryOverScheduler::class.java)
    }

    /**
     * 매일 23:50 KST에 실행.
     *
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    fun run() {
        val today = LocalDate.now()
        log.info("[랭킹] Score Carry-Over 스케줄 시작 [today={}]", today)
        rankingCarryOverService.carryOverToNextDay(today)
    }
}
