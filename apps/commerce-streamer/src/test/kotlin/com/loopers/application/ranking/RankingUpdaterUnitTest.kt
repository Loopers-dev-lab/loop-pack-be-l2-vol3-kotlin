package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEvent
import com.loopers.domain.ranking.RankingKeyPolicy
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.ScoreCalculator
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class RankingUpdaterUnitTest {

    private val repository: RankingRepository = mockk(relaxed = true)
    private val calculator: ScoreCalculator = mockk()
    private val fixedClock: Clock = Clock.fixed(
        LocalDate.of(2026, 4, 8).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault(),
    )

    private lateinit var updater: RankingUpdater
    private val expectedKey = "ranking:all:20260408"

    @BeforeEach
    fun setUp() {
        clearMocks(repository, calculator)
        every { repository.expire(any(), any()) } just Runs
        every { repository.incrementScore(any(), any(), any()) } just Runs
        updater = RankingUpdater(repository, calculator, fixedClock)
    }

    @Nested
    @DisplayName("applyEvent()")
    inner class ApplyEvent {

        @Test
        fun `오늘 일자 키에 ScoreCalculator 가 계산한 델타로 ZINCRBY 가 호출된다`() {
            // Arrange
            val event = RankingEvent.Viewed(productId = 100L)
            every { calculator.scoreFor(event) } returns 0.1

            // Act
            updater.applyEvent(event)

            // Assert
            verify(exactly = 1) { repository.incrementScore(expectedKey, 100L, 0.1) }
        }

        @Test
        fun `같은 키에 여러 이벤트가 들어오면 EXPIRE 는 첫 호출에서만 1회 발생한다`() {
            every { calculator.scoreFor(any()) } returns 0.1

            updater.applyEvent(RankingEvent.Viewed(100L))
            updater.applyEvent(RankingEvent.Viewed(101L))
            updater.applyEvent(RankingEvent.Viewed(102L))

            verify(exactly = 3) { repository.incrementScore(any(), any(), any()) }
            verify(exactly = 1) { repository.expire(expectedKey, RankingKeyPolicy.TTL) }
        }

        @Test
        fun `델타가 0 이면 Redis 호출을 생략한다 (불필요한 트래픽 차단)`() {
            val event = RankingEvent.Viewed(productId = 100L)
            every { calculator.scoreFor(event) } returns 0.0

            updater.applyEvent(event)

            verify(exactly = 0) { repository.incrementScore(any(), any(), any()) }
            verify(exactly = 0) { repository.expire(any(), any()) }
        }

        @Test
        fun `Ordered 이벤트도 정상적으로 ZINCRBY 가 호출된다`() {
            val event = RankingEvent.Ordered(productId = 200L, amount = 50_000L)
            every { calculator.scoreFor(event) } returns 7.65

            updater.applyEvent(event)

            verify(exactly = 1) { repository.incrementScore(expectedKey, 200L, 7.65) }
        }

        @Test
        fun `음수 델타 (Unliked) 도 ZINCRBY 로 정상 반영된다`() {
            val event = RankingEvent.Unliked(productId = 100L)
            every { calculator.scoreFor(event) } returns -0.2

            updater.applyEvent(event)

            verify(exactly = 1) { repository.incrementScore(expectedKey, 100L, -0.2) }
        }
    }

    @Nested
    @DisplayName("일자 변경 시나리오")
    inner class DateRollover {

        @Test
        fun `clock 이 다른 일자를 가리키면 새 키에 대해 EXPIRE 가 다시 1회 호출된다`() {
            every { calculator.scoreFor(any()) } returns 0.1

            // Day 1
            val day1Updater = RankingUpdater(
                repository,
                calculator,
                Clock.fixed(
                    LocalDate.of(2026, 4, 8).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault(),
                ),
            )
            day1Updater.applyEvent(RankingEvent.Viewed(100L))
            day1Updater.applyEvent(RankingEvent.Viewed(101L))

            // Day 2 (새 instance — 실제로는 시간이 흘러 LocalDate 가 바뀌는 시나리오)
            val day2Updater = RankingUpdater(
                repository,
                calculator,
                Clock.fixed(
                    LocalDate.of(2026, 4, 9).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault(),
                ),
            )
            day2Updater.applyEvent(RankingEvent.Viewed(100L))

            // Assert
            verify(exactly = 1) { repository.expire("ranking:all:20260408", RankingKeyPolicy.TTL) }
            verify(exactly = 1) { repository.expire("ranking:all:20260409", RankingKeyPolicy.TTL) }
        }
    }
}
