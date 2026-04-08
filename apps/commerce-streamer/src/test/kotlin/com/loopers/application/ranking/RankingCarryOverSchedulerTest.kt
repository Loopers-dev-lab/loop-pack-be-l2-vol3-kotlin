package com.loopers.application.ranking

import com.loopers.zset.RedisZSetTemplate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RankingCarryOverSchedulerTest {

    private val redisZSetTemplate: RedisZSetTemplate = mockk(relaxed = true)
    private val rankingProperties = RankingProperties(
        weight = RankingProperties.Weight(),
        ttlDays = 2,
        carryOverWeight = 0.1,
    )
    private val scheduler = RankingCarryOverScheduler(redisZSetTemplate, rankingProperties)

    @DisplayName("Score Carry-Over 스케줄러")
    @Nested
    inner class CarryOver {

        @DisplayName("전날 점수의 10%를 오늘 키에 복사한다")
        @Test
        fun carryOverWithWeight() {
            every { redisZSetTemplate.unionStoreWithWeight(any(), any(), 0.1) } returns 50L

            scheduler.carryOver()

            verify { redisZSetTemplate.unionStoreWithWeight(any(), any(), 0.1) }
            verify { redisZSetTemplate.setTtlIfAbsent(any(), any()) }
        }

        @DisplayName("Redis 장애 시 예외가 전파되지 않는다")
        @Test
        fun redisFailureDoesNotCrash() {
            every { redisZSetTemplate.unionStoreWithWeight(any(), any(), any()) } throws RuntimeException("Redis down")

            scheduler.carryOver()
        }
    }
}
