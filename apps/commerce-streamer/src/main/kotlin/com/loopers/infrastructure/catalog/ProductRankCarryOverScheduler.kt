package com.loopers.infrastructure.catalog

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 랭킹 score carry-over 스케줄러.
 *
 * 매일 23:50에 오늘 rank:all의 score를 carry-over 비율(기본 0.1)만큼
 * 내일 키에 합산하여 콜드스타트를 방지한다.
 *
 * 23:50 실행의 트레이드오프:
 * - 장점: 다음 날 자정 직후에도 어제 인기 상품이 낮은 점수로 보임 (UX)
 * - 단점: 마지막 10분의 이벤트가 carry-over에 미반영 — 10분 × 0.1 = 무시 가능한 수준
 */
@Component
class ProductRankCarryOverScheduler(
    private val productRankRedisRepository: ProductRankRedisRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 50 23 * * *")
    fun execute() {
        val today = LocalDate.now()
        log.info("랭킹 carry-over 시작 — {} → {}", today, today.plusDays(1))
        productRankRedisRepository.carryOver(today)
        log.info("랭킹 carry-over 완료")
    }
}
