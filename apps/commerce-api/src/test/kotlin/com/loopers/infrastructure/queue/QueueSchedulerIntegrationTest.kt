package com.loopers.infrastructure.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("QueueScheduler 통합 테스트")
@SpringBootTest
class QueueSchedulerIntegrationTest
@Autowired
constructor(
    private val queueScheduler: QueueScheduler,
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("Lua 스크립트로 pop + issue가 배치 실행된다")
    inner class PopAndIssue {
        @Test
        @DisplayName("대기열 30명 → 21명 pop + 토큰 발급, 9명 잔여")
        fun popAndIssue_batch() {
            (1L..30L).forEach { waitingQueueRepository.enter(it) }

            queueScheduler.processQueue()

            assertAll(
                { assertThat(waitingQueueRepository.size()).isEqualTo(9) },
                { (1L..21L).forEach { assertThat(entryTokenRepository.exists(it)).isTrue() } },
                { (22L..30L).forEach { assertThat(entryTokenRepository.exists(it)).isFalse() } },
            )
        }

        @Test
        @DisplayName("빈 대기열 → 빈 결과")
        fun popAndIssue_emptyQueue() {
            queueScheduler.processQueue()

            assertThat(waitingQueueRepository.size()).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("분산 락이 다중 실행을 방지한다")
    inner class DistributedLock {
        @Test
        @DisplayName("2스레드 동시 실행 → 1회만 pop 발생")
        fun lock_singleExecution() {
            (1L..42L).forEach { waitingQueueRepository.enter(it) }

            val latch = CountDownLatch(2)
            val executor = Executors.newFixedThreadPool(2)
            val executionCount = AtomicInteger(0)

            repeat(2) {
                executor.submit {
                    try {
                        queueScheduler.processQueue()
                        executionCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            val remaining = waitingQueueRepository.size()
            assertThat(remaining).isEqualTo(21)
        }
    }

    @Nested
    @DisplayName("배치 크기를 초과하는 대기열에서 안정적으로 동작한다")
    inner class OverCapacity {
        @Test
        @DisplayName("대기열 1000명, 배치 21 → 21명만 처리, 나머지 유지")
        fun schedule_overCapacity() {
            (1L..1000L).forEach { waitingQueueRepository.enter(it) }

            queueScheduler.processQueue()

            assertThat(waitingQueueRepository.size()).isEqualTo(979)
        }
    }
}
