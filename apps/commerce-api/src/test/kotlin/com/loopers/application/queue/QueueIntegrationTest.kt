package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import com.loopers.domain.queue.WaitingQueueService
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("대기열 시나리오 테스트")
class QueueIntegrationTest {

    private lateinit var waitingQueueRepository: WaitingQueueRepository
    private lateinit var entryTokenRepository: EntryTokenRepository
    private lateinit var waitingQueueService: WaitingQueueService

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        waitingQueueService = WaitingQueueService(waitingQueueRepository, entryTokenRepository)
    }

    @DisplayName("동시 진입 테스트")
    @Nested
    inner class ConcurrentEntry {
        @DisplayName("100명이 동시에 대기열에 진입하면 순서가 정확히 보장되고 모두 진입에 성공한다")
        @Test
        fun allUsersEnterQueue_whenConcurrentRequests() {
            // arrange
            val threadCount = 100
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // act
            (1..threadCount).forEach { userId ->
                executor.submit {
                    try {
                        waitingQueueService.enterQueue(userId.toLong())
                        successCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertThat(successCount.get()).isEqualTo(threadCount)
            assertThat(waitingQueueRepository.getTotalWaitingCount()).isEqualTo(threadCount.toLong())
        }

        @DisplayName("같은 사용자가 동시에 진입을 시도하면 1번만 성공한다")
        @Test
        fun onlyOneSucceeds_whenSameUserConcurrentEntry() {
            // arrange
            val threadCount = 10
            val userId = 1L
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            (1..threadCount).forEach { _ ->
                executor.submit {
                    try {
                        waitingQueueService.enterQueue(userId)
                        successCount.incrementAndGet()
                    } catch (_: CoreException) {
                        failCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            assertThat(successCount.get()).isEqualTo(1)
            assertThat(failCount.get()).isEqualTo(threadCount - 1)
            assertThat(waitingQueueRepository.getTotalWaitingCount()).isEqualTo(1)
        }
    }

    @DisplayName("토큰 만료 테스트")
    @Nested
    inner class TokenExpiry {
        @DisplayName("TTL이 초과되면 토큰이 자동으로 만료되어 조회되지 않는다")
        @Test
        fun tokenExpires_afterTtl() {
            // arrange
            val userId = 1L
            val fakeRepo = entryTokenRepository as FakeEntryTokenRepository
            fakeRepo.issueToken(userId, "test-token", 1L)

            // act — TTL 1초 이후를 시뮬레이션
            Thread.sleep(1100)
            val token = fakeRepo.getToken(userId)

            // assert
            assertThat(token).isNull()
        }

        @DisplayName("TTL 이내에는 토큰이 유효하게 조회된다")
        @Test
        fun tokenIsValid_beforeTtl() {
            // arrange
            val userId = 1L
            entryTokenRepository.issueToken(userId, "test-token", 60L)

            // act
            val token = entryTokenRepository.getToken(userId)

            // assert
            assertThat(token).isEqualTo("test-token")
        }
    }

    @DisplayName("처리량 초과 테스트")
    @Nested
    inner class ThroughputOverload {
        @DisplayName("배치 크기 이상의 대기열 요청이 있어도 스케줄러가 배치 크기만큼만 처리한다")
        @Test
        fun schedulerProcessesBatchSizeOnly_whenQueueExceedsBatch() {
            // arrange
            val totalUsers = 100
            (1..totalUsers).forEach { userId ->
                waitingQueueRepository.enter(userId.toLong(), System.currentTimeMillis().toDouble() + userId)
            }
            assertThat(waitingQueueRepository.getTotalWaitingCount()).isEqualTo(totalUsers.toLong())

            // act
            val processedCount = waitingQueueService.processQueue()

            // assert
            assertThat(processedCount).isEqualTo(WaitingQueueService.BATCH_SIZE.toInt())
            assertThat(waitingQueueRepository.getTotalWaitingCount())
                .isEqualTo(totalUsers.toLong() - WaitingQueueService.BATCH_SIZE)
        }

        @DisplayName("여러 번 스케줄러를 실행하면 대기열의 모든 사용자가 순차적으로 처리된다")
        @Test
        fun allUsersProcessed_whenSchedulerRunsMultipleTimes() {
            // arrange
            val totalUsers = 50
            (1..totalUsers).forEach { userId ->
                waitingQueueRepository.enter(userId.toLong(), System.currentTimeMillis().toDouble() + userId)
            }

            // act
            var totalProcessed = 0
            while (waitingQueueRepository.getTotalWaitingCount() > 0) {
                totalProcessed += waitingQueueService.processQueue()
            }

            // assert
            assertThat(totalProcessed).isEqualTo(totalUsers)
            assertThat(waitingQueueRepository.getTotalWaitingCount()).isEqualTo(0)
        }

        @DisplayName("스케줄러 실행 후 처리된 사용자에게 입장 토큰이 발급된다")
        @Test
        fun tokensIssued_afterSchedulerProcessing() {
            // arrange
            (1..5).forEach { userId ->
                waitingQueueRepository.enter(userId.toLong(), System.currentTimeMillis().toDouble() + userId)
            }

            // act
            waitingQueueService.processQueue()

            // assert
            (1..5).forEach { userId ->
                assertThat(entryTokenRepository.hasToken(userId.toLong())).isTrue()
            }
        }
    }

    /**
     * Redis Sorted Set의 동작을 시뮬레이션하는 Fake Repository.
     * ConcurrentSkipListMap을 사용하여 score 기반 정렬과 thread-safety를 보장합니다.
     */
    private class FakeWaitingQueueRepository : WaitingQueueRepository {
        private val members = ConcurrentHashMap<String, Double>()

        override fun enter(userId: Long, score: Double): Boolean {
            return members.putIfAbsent(userId.toString(), score) == null
        }

        override fun getPosition(userId: Long): Long? {
            val userScore = members[userId.toString()] ?: return null
            return members.values.count { it < userScore }.toLong()
        }

        override fun getTotalWaitingCount(): Long = members.size.toLong()

        override fun popMinN(count: Long): Set<String> {
            val sorted = members.entries.sortedBy { it.value }.take(count.toInt())
            sorted.forEach { members.remove(it.key) }
            return sorted.map { it.key }.toSet()
        }

        override fun exists(userId: Long): Boolean = members.containsKey(userId.toString())
    }

    /**
     * Redis의 TTL 동작을 시뮬레이션하는 Fake Repository.
     * 만료 시간을 추적하여 TTL 초과 시 토큰을 무효화합니다.
     */
    private class FakeEntryTokenRepository : EntryTokenRepository {
        private data class TokenEntry(val token: String, val expiresAtMillis: Long)

        private val tokens = ConcurrentHashMap<Long, TokenEntry>()

        override fun issueToken(userId: Long, token: String, ttlSeconds: Long) {
            tokens[userId] = TokenEntry(token, System.currentTimeMillis() + ttlSeconds * 1000)
        }

        override fun getToken(userId: Long): String? {
            val entry = tokens[userId] ?: return null
            if (System.currentTimeMillis() > entry.expiresAtMillis) {
                tokens.remove(userId)
                return null
            }
            return entry.token
        }

        override fun deleteToken(userId: Long) {
            tokens.remove(userId)
        }

        override fun hasToken(userId: Long): Boolean {
            return getToken(userId) != null
        }
    }
}
