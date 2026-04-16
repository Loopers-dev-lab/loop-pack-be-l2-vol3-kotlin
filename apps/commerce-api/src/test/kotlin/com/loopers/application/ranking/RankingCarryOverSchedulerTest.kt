package com.loopers.application.ranking

import com.loopers.infrastructure.ranking.RankingRedisReader
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class RankingCarryOverSchedulerTest {
    private val rankingRedisReader = mockk<RankingRedisReader>(relaxed = true)

    @Test
    fun `carry_over는_기준일의_상위_랭킹을_다음날로_복사한다`() {
        val scheduler = RankingCarryOverScheduler(
            rankingCarryOverProperties = RankingCarryOverProperties(),
            rankingRedisReader = rankingRedisReader,
        )

        scheduler.warmUp(ZonedDateTime.parse("2026-04-10T23:50:00+09:00[Asia/Seoul]"))

        verify(exactly = 1) {
            rankingRedisReader.carryOver(
                fromDate = "20260410",
                toDate = "20260411",
                limit = 20L,
                multiplier = 0.2,
            )
        }
        confirmVerified(rankingRedisReader)
    }

    @Test
    fun `carry_over가_비활성화되면_복사하지_않는다`() {
        val scheduler = RankingCarryOverScheduler(
            rankingCarryOverProperties = RankingCarryOverProperties(enabled = false),
            rankingRedisReader = rankingRedisReader,
        )

        scheduler.warmUp(ZonedDateTime.parse("2026-04-10T23:50:00+09:00[Asia/Seoul]"))

        verify(exactly = 0) { rankingRedisReader.carryOver(any(), any(), any(), any()) }
    }
}
