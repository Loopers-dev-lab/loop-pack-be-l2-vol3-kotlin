package com.loopers.application.metrics

import com.loopers.domain.ranking.FakeRankingScoreRepository
import com.loopers.domain.ranking.RankingWeight
import com.loopers.domain.ranking.repository.FailedScoreUpdateRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Clock
import java.time.LocalDate

/**
 * T4: afterCommit Redis 실패 → FailedScoreUpdate 잔존 → 재시도 스케줄러 회복 전체 플로우 검증.
 *
 * - 실제 RedisRankingScoreRepository 대신 FakeRankingScoreRepository 를 @Primary 로 오버라이드해
 *   Redis 호출 실패 시점을 결정적으로 제어한다.
 * - @Scheduled 자동 실행은 properties 로 막고, retry() 를 수동 호출해 시나리오를 격리한다.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.task.scheduling.enabled=false",
        "ranking.retry.interval-ms=86400000",
    ],
)
@Import(RedisFailureRecoveryIntegrationTest.FakeRankingScoreConfig::class)
class RedisFailureRecoveryIntegrationTest @Autowired constructor(
    private val useCase: UpdateProductMetricsUseCase,
    private val retryScheduler: RetryFailedScoreUpdateScheduler,
    private val failedScoreUpdateRepository: FailedScoreUpdateRepository,
    private val fakeRankingScoreRepository: FakeRankingScoreRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
) {

    @TestConfiguration
    class FakeRankingScoreConfig {
        @Bean
        @Primary
        fun fakeRankingScoreRepository(): FakeRankingScoreRepository = FakeRankingScoreRepository()
    }

    @BeforeEach
    fun setUp() {
        fakeRankingScoreRepository.clear()
        jdbcTemplate.update(
            "INSERT INTO products (id) VALUES (1)",
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        fakeRankingScoreRepository.clear()
    }

    @Test
    @DisplayName("LIKE_ADDED 처리 직후 Redis가 실패해도 다음 retry 주기에 점수가 회복되고 FailedScoreUpdate가 정리된다")
    fun `afterCommit Redis 실패 후 retry 스케줄러가 회복시킨다`() {
        // Arrange
        val productId = 1L
        val today = LocalDate.now(clock)
        // 첫 호출만 실패, 재시도는 성공
        fakeRankingScoreRepository.failuresRemaining = 1

        // Act 1: catalog 이벤트 처리 → DB 커밋 → afterCommit Redis 실패 → FailedScoreUpdate 잔존
        useCase.handleCatalogEvent(
            eventId = "evt-recovery-1",
            eventType = UpdateProductMetricsUseCase.LIKE_ADDED,
            productId = productId,
        )

        // Assert 중간: Redis 미반영, FailedScoreUpdate 1건 잔존
        assertThat(fakeRankingScoreRepository.getScore(productId, today)).isEqualTo(0.0)
        val pendingBeforeRetry = failedScoreUpdateRepository.findPendingUpdates(maxRetryCount = 10, limit = 100)
        assertThat(pendingBeforeRetry).hasSize(1)
        assertThat(pendingBeforeRetry[0].eventId).isEqualTo("evt-recovery-1")
        assertThat(pendingBeforeRetry[0].productId).isEqualTo(productId)
        assertThat(pendingBeforeRetry[0].score).isCloseTo(RankingWeight.LIKE, Offset.offset(0.001))
        assertThat(pendingBeforeRetry[0].rankingDate).isEqualTo(today)

        // Act 2: 스케줄러 수동 호출 — failuresRemaining 이 0이므로 이번엔 성공
        retryScheduler.retry()

        // Assert 최종: Fake 에 점수 반영 + FailedScoreUpdate 레코드 정리
        assertThat(fakeRankingScoreRepository.getScore(productId, today))
            .isCloseTo(RankingWeight.LIKE, Offset.offset(0.001))
        assertThat(failedScoreUpdateRepository.findPendingUpdates(maxRetryCount = 10, limit = 100)).isEmpty()
    }
}
