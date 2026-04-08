package com.loopers.application.ranking

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RankingEventListenerTest {

    private val rankingScoreAccumulator: RankingScoreAccumulator = mockk(relaxed = true)
    private val listener = RankingEventListener(rankingScoreAccumulator)

    @DisplayName("랭킹 이벤트 리스너")
    @Nested
    inner class Handle {

        @DisplayName("LikeAdded 이벤트 수신 시 addLikeScore를 호출한다")
        @Test
        fun likeAdded() {
            listener.handle(RankingScoreEvent.LikeAdded(101L))

            verify { rankingScoreAccumulator.addLikeScore(101L) }
        }

        @DisplayName("LikeCancelled 이벤트 수신 시 cancelLikeScore를 호출한다")
        @Test
        fun likeCancelled() {
            listener.handle(RankingScoreEvent.LikeCancelled(101L))

            verify { rankingScoreAccumulator.cancelLikeScore(101L) }
        }

        @DisplayName("OrderCreated 이벤트 수신 시 addOrderScore를 호출한다")
        @Test
        fun orderCreated() {
            listener.handle(RankingScoreEvent.OrderCreated(listOf(101L, 202L), 30000L))

            verify { rankingScoreAccumulator.addOrderScore(listOf(101L, 202L), 30000L) }
        }

        @DisplayName("Redis 장애 시 예외가 전파되지 않는다")
        @Test
        fun redisFailureDoesNotPropagate() {
            every { rankingScoreAccumulator.addLikeScore(any()) } throws RuntimeException("Redis down")

            listener.handle(RankingScoreEvent.LikeAdded(101L))

            verify { rankingScoreAccumulator.addLikeScore(101L) }
        }
    }
}
