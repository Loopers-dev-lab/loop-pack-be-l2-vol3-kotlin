package com.loopers.application.metrics

import com.loopers.domain.ranking.FakeFailedScoreUpdateRepository
import com.loopers.domain.ranking.FakeRankingScoreRepository
import com.loopers.domain.ranking.RankingWeight
import com.loopers.domain.ranking.model.FailedScoreUpdate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RetryFailedScoreUpdateSchedulerTest {

    private lateinit var failedScoreUpdateRepository: FakeFailedScoreUpdateRepository
    private lateinit var rankingScoreRepository: FakeRankingScoreRepository
    private lateinit var scheduler: RetryFailedScoreUpdateScheduler

    @BeforeEach
    fun setUp() {
        failedScoreUpdateRepository = FakeFailedScoreUpdateRepository()
        rankingScoreRepository = FakeRankingScoreRepository()
        scheduler = RetryFailedScoreUpdateScheduler(
            failedScoreUpdateRepository,
            rankingScoreRepository,
        )
    }

    private val rankingDate = LocalDate.of(2026, 4, 7)

    @Test
    @DisplayName("미처리 건을 재처리하고 성공 시 삭제한다")
    fun `미처리 건을 재처리하고 성공 시 삭제한다`() {
        // Arrange
        failedScoreUpdateRepository.save(
            FailedScoreUpdate(eventId = "evt-1", productId = 1L, score = RankingWeight.VIEW, rankingDate = rankingDate),
        )

        // Act
        scheduler.retry()

        // Assert — Redis 점수 반영 + 테이블에서 삭제
        assertThat(rankingScoreRepository.getScore(1L))
            .isCloseTo(RankingWeight.VIEW, Offset.offset(0.001))
        assertThat(failedScoreUpdateRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("재처리 실패 시 retryCount를 증가시키고 유지한다")
    fun `재처리 실패 시 retryCount를 증가시키고 유지한다`() {
        // Arrange
        failedScoreUpdateRepository.save(
            FailedScoreUpdate(eventId = "evt-1", productId = 1L, score = RankingWeight.VIEW, rankingDate = rankingDate),
        )
        rankingScoreRepository.failuresRemaining = 10

        // Act
        scheduler.retry()

        // Assert — 삭제되지 않고 retryCount 증가
        val remaining = failedScoreUpdateRepository.findAll()
        assertThat(remaining).hasSize(1)
        assertThat(remaining[0].retryCount).isEqualTo(1)
    }

    @Test
    @DisplayName("Redis 반영 성공 후 delete 실패 시 재처리해도 점수가 중복 가산되지 않는다")
    fun `Redis OK delete 실패 후 재처리 시 점수 중복 없이 레코드가 정리된다`() {
        // Arrange
        failedScoreUpdateRepository.save(
            FailedScoreUpdate(eventId = "evt-1", productId = 1L, score = RankingWeight.VIEW, rankingDate = rankingDate),
        )
        failedScoreUpdateRepository.deleteFailuresRemaining = 1 // 첫 번째 delete만 실패

        // Act — 1차 재처리: score 반영 성공 + delete 실패 → retryCount 증가
        scheduler.retry()

        // Assert 중간: 점수는 반영됐지만 레코드가 잔존
        assertThat(rankingScoreRepository.getScore(1L)).isCloseTo(RankingWeight.VIEW, Offset.offset(0.001))
        assertThat(failedScoreUpdateRepository.findAll()).hasSize(1)

        // Act — 2차 재처리: incrementScore 멱등성으로 no-op, delete 성공
        scheduler.retry()

        // Assert 최종: 점수 중복 가산 없음, 레코드 정리됨
        assertThat(rankingScoreRepository.getScore(1L)).isCloseTo(RankingWeight.VIEW, Offset.offset(0.001))
        assertThat(failedScoreUpdateRepository.findAll()).isEmpty()
    }

    @Test
    @DisplayName("maxRetryCount를 초과한 건은 조회되지 않는다")
    fun `maxRetryCount를 초과한 건은 조회되지 않는다`() {
        // Arrange — retryCount가 이미 10인 건
        val failed = FailedScoreUpdate(
            eventId = "evt-1",
            productId = 1L,
            score = RankingWeight.VIEW,
            rankingDate = rankingDate,
            retryCount = 10,
        )
        failedScoreUpdateRepository.save(failed)

        // Act
        scheduler.retry()

        // Assert — 재처리 대상이 아니므로 점수 미반영, 테이블에 그대로
        assertThat(rankingScoreRepository.getScore(1L)).isEqualTo(0.0)
        assertThat(failedScoreUpdateRepository.findAll()).hasSize(1)
    }
}
