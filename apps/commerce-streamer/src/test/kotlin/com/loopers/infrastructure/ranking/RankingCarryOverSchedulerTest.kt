package com.loopers.infrastructure.ranking

import com.loopers.domain.ranking.ProductRankingRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("RankingCarryOverScheduler")
class RankingCarryOverSchedulerTest {
    private val productRankingRepository: ProductRankingRepository = mock()
    private val scheduler = RankingCarryOverScheduler(productRankingRepository)

    @Nested
    @DisplayName("오늘 ZSET이 존재하지 않거나 비어있으면 carry-over를 건너뛴다")
    inner class SkipEmpty {

        @Test
        @DisplayName("오늘 키가 존재하지 않으면 carryOver를 호출하지 않는다")
        fun runCarryOver_noSourceKey_skips() {
            whenever(productRankingRepository.exists(any())).thenReturn(false)

            scheduler.runCarryOver()

            verify(productRankingRepository, never()).carryOver(any(), any(), any())
        }
    }

    @Nested
    @DisplayName("오늘 ZSET이 존재하면 carry-over를 실행한다")
    inner class ExecuteCarryOver {

        @Test
        @DisplayName("carryOver를 weight=0.1로 호출한다")
        fun runCarryOver_success() {
            whenever(productRankingRepository.exists(any())).thenReturn(true)

            scheduler.runCarryOver()

            verify(productRankingRepository).carryOver(
                any(),
                any(),
                eq(RankingCarryOverScheduler.CARRY_OVER_WEIGHT),
            )
        }
    }

    @Nested
    @DisplayName("carry-over 실패 시 이벤트 처리에 영향을 주지 않는다")
    inner class FailureHandling {

        @Test
        @DisplayName("carryOver에서 예외 발생 → 예외가 전파되지 않는다")
        fun runCarryOver_failure_doesNotThrow() {
            whenever(productRankingRepository.exists(any())).thenReturn(true)
            whenever(productRankingRepository.carryOver(any(), any(), any()))
                .thenThrow(RuntimeException("Redis connection refused"))

            scheduler.runCarryOver()
        }
    }
}
