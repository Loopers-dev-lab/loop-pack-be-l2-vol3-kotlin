package com.loopers.domain.ranking

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RankingCarryOverService(
    private val rankingCarryOverRepository: RankingCarryOverRepository,
    @Value("\${ranking.carry-over.weight:0.1}") private val carryOverWeight: Double,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * baseDate의 랭킹 점수 일부를 다음 날 키에 미리 복사하여 콜드 스타트를 완화한다.
     *
     * @param baseDate 복사 원본이 되는 날짜 (보통 오늘)
     * @return 복사된 멤버 수
     */
    fun execute(baseDate: LocalDate): Long {
        val sourceKey = RankingKeyGenerator.dailyKey(baseDate)
        val destKey = RankingKeyGenerator.dailyKey(baseDate.plusDays(1))

        log.info(
            "ranking_carry_over_start sourceKey={} destKey={} weight={}",
            sourceKey,
            destKey,
            carryOverWeight,
        )

        val count = rankingCarryOverRepository.carryOver(sourceKey, destKey, carryOverWeight)

        log.info(
            "ranking_carry_over_complete sourceKey={} destKey={} copiedMembers={}",
            sourceKey,
            destKey,
            count,
        )

        return count
    }
}
