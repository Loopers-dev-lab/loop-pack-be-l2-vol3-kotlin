package com.loopers.interfaces.support.scheduler

import com.loopers.application.queue.IssueEntryTokensUseCase
import com.loopers.application.queue.QueueFallbackHandler
import com.loopers.application.queue.QueueProperties
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.FakeQueueTokenBatchProcessor
import com.loopers.domain.queue.QueueTokenBatchProcessor
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.RedisConnectionFailureException

class QueueSchedulerTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var queueFallbackHandler: QueueFallbackHandler
    private lateinit var scheduler: QueueScheduler

    private val properties = QueueProperties(
        maxCapacity = 50_000,
        batchSize = 3,
        tokenTtlSeconds = 300,
        schedulerDelayMs = 100,
    )

    @BeforeEach
    fun setUp() {
        entryTokenRepository = FakeEntryTokenRepository()
        waitingQueueRepository = FakeWaitingQueueRepository()
        queueFallbackHandler = QueueFallbackHandler()
        val issueUseCase = IssueEntryTokensUseCase(
            FakeQueueTokenBatchProcessor(waitingQueueRepository, entryTokenRepository),
            properties,
        )
        scheduler = QueueScheduler(issueUseCase, queueFallbackHandler)
    }

    @Nested
    @DisplayName("issueTokens 실행 시")
    inner class IssueTokens {

        @Test
        @DisplayName("대기열에 유저가 있으면 batchSize만큼 토큰이 발급된다")
        fun issueTokens_issuesTokensUpToBatchSize() {
            // arrange — 5명 진입, batchSize=3
            (1L..5L).forEach { waitingQueueRepository.enter(UserId(it), 50_000) }

            // act
            scheduler.issueTokens()

            // assert — 3명에게 토큰 발급, 잔여 2명이 대기열에 남아 있음
            val issuedCount = (1L..5L).count { entryTokenRepository.find(UserId(it)) != null }
            assertThat(issuedCount).isEqualTo(3)
            assertThat(waitingQueueRepository.count()).isEqualTo(2)
        }

        @Test
        @DisplayName("대기열이 비어 있으면 아무 동작도 하지 않는다")
        fun issueTokens_emptyQueue_noOp() {
            // act
            scheduler.issueTokens()

            // assert — 예외 없이 정상 종료, 발급된 토큰 없음
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
            val issuedCount = (1L..10L).count { entryTokenRepository.find(UserId(it)) != null }
            assertThat(issuedCount).isEqualTo(0)
        }

        @Test
        @DisplayName("대기 인원이 batchSize 미만이면 전원에게 토큰이 발급된다")
        fun issueTokens_fewerThanBatchSize_allIssued() {
            // arrange — batchSize=3 이지만 2명만 진입
            (1L..2L).forEach { waitingQueueRepository.enter(UserId(it), 50_000) }

            // act
            scheduler.issueTokens()

            // assert — 2명 전원에게 토큰 발급, 대기열 비어있음
            val issuedCount = (1L..2L).count { entryTokenRepository.find(UserId(it)) != null }
            assertThat(issuedCount).isEqualTo(2)
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("대기 인원이 batchSize와 동일하면 전원에게 토큰이 발급된다")
        fun issueTokens_exactlyBatchSize_allIssued() {
            // arrange — batchSize=3, 3명 진입
            (1L..3L).forEach { waitingQueueRepository.enter(UserId(it), 50_000) }

            // act
            scheduler.issueTokens()

            // assert — 3명 전원에게 토큰 발급, 대기열 비어있음
            val issuedCount = (1L..3L).count { entryTokenRepository.find(UserId(it)) != null }
            assertThat(issuedCount).isEqualTo(3)
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("Redis 장애 발생 시 fallback 상태로 전환된다")
        fun issueTokens_redisFailure_marksFallback() {
            // arrange
            val failingProcessor = object : QueueTokenBatchProcessor {
                override fun popAndIssueTokens(count: Int, ttlSeconds: Long) =
                    throw RedisConnectionFailureException("Redis connection refused")
            }
            val failingIssueUseCase = IssueEntryTokensUseCase(failingProcessor, properties)
            val failingScheduler = QueueScheduler(failingIssueUseCase, queueFallbackHandler)

            // act — 예외 없이 정상 종료
            failingScheduler.issueTokens()

            // assert
            assertThat(queueFallbackHandler.isAvailable()).isFalse()
        }

        @Test
        @DisplayName("비인프라 예외(NPE 등) 발생 시 fallback 상태가 변하지 않고 예외가 전파된다")
        fun issueTokens_nonInfraException_rethrows() {
            // arrange
            val npeProcessor = object : QueueTokenBatchProcessor {
                override fun popAndIssueTokens(count: Int, ttlSeconds: Long) =
                    throw NullPointerException("unexpected null")
            }
            val npeIssueUseCase = IssueEntryTokensUseCase(npeProcessor, properties)
            val npeScheduler = QueueScheduler(npeIssueUseCase, queueFallbackHandler)

            // act & assert
            assertThrows<NullPointerException> {
                npeScheduler.issueTokens()
            }
            assertThat(queueFallbackHandler.isAvailable()).isTrue()
        }

        @Test
        @DisplayName("fallback 상태에서 Redis 복구 시 available 상태로 전환된다")
        fun issueTokens_recoveryAfterFallback_marksAvailable() {
            // arrange — fallback 상태로 전환
            queueFallbackHandler.markUnavailable()
            assertThat(queueFallbackHandler.isAvailable()).isFalse()

            // act — 정상 실행 (빈 대기열)
            scheduler.issueTokens()

            // assert
            assertThat(queueFallbackHandler.isAvailable()).isTrue()
        }
    }
}
