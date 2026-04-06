package com.loopers.concurrency

import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueueService
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Import(RedisTestContainersConfig::class, MySqlTestContainersConfig::class)
class QueueConcurrencyTest @Autowired constructor(
    private val queueService: QueueService,
    private val queueRepository: QueueRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    /**
     * 액티브 토큰 슬롯(175개)을 꽉 채워 이후 진입자가 대기열로 유도되도록 한다.
     * userId 99000~ 은 테스트 대상 userId(1~100)와 겹치지 않는다.
     */
    private fun fillActiveTokenSlots() {
        repeat(175) { i -> queueService.enterQueue(99000L + i) }
    }

    @Nested
    @DisplayName("대기열 동시 진입")
    inner class ConcurrentEnterQueue {

        @Test
        @Timeout(30, unit = TimeUnit.SECONDS)
        @DisplayName("100명이 동시에 대기열에 진입하면 중복 없이 유니크한 순번이 보장된다")
        fun concurrentEnterQueueGuaranteesUniqueRanks() {
            // arrange — 액티브 토큰 슬롯을 꽉 채워 대기열 모드로 진입하도록 유도
            fillActiveTokenSlots()

            val threadCount = 100
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act — 100명 동시 진입
            repeat(threadCount) { i ->
                val userId = (i + 1).toLong()
                executorService.submit {
                    try {
                        startLatch.await()
                        queueService.enterQueue(userId)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert — 100명 모두 진입 성공, 대기열 크기 정확히 100
            assertAll(
                { assertThat(successCount.get()).isEqualTo(threadCount) },
                { assertThat(failCount.get()).isEqualTo(0) },
                { assertThat(queueRepository.size()).isEqualTo(threadCount.toLong()) },
            )
        }

        @Test
        @Timeout(30, unit = TimeUnit.SECONDS)
        @DisplayName("같은 유저가 동시에 여러 번 진입 시도해도 대기열에 1번만 등록된다")
        fun sameUserConcurrentEnterIsIdempotent() {
            // arrange
            fillActiveTokenSlots()

            val userId = 1L
            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)

            // act — 같은 userId로 20번 동시 진입 시도
            repeat(threadCount) {
                executorService.submit {
                    try {
                        startLatch.await()
                        queueService.enterQueue(userId)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        // QUEUE_FULL 등 예외는 무시
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert — ZADD NX로 인해 대기열에 userId가 딱 1개만 존재
            assertAll(
                { assertThat(successCount.get()).isEqualTo(threadCount) },
                { assertThat(queueRepository.getRank(userId)).isNotNull() },
                { assertThat(queueRepository.size()).isEqualTo(1L) },
            )
        }

        @Test
        @Timeout(30, unit = TimeUnit.SECONDS)
        @DisplayName("100명이 동시에 진입하면 순번이 1~100 사이로 겹치지 않는다")
        fun concurrentEnterQueueHasNoDuplicateRanks() {
            // arrange
            fillActiveTokenSlots()

            val threadCount = 100
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(threadCount)
            val positions = mutableListOf<Long>()
            val lock = Any()

            // act
            repeat(threadCount) { i ->
                val userId = (i + 1).toLong()
                executorService.submit {
                    try {
                        startLatch.await()
                        val info = queueService.enterQueue(userId)
                        synchronized(lock) { positions.add(info.position) }
                    } catch (e: Exception) {
                        // ignore
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert — 순번이 모두 유니크 (Redis Sorted Set + ZADD NX 보장)
            val minPosition: Long = positions.minOrNull() ?: 0L
            val maxPosition: Long = positions.maxOrNull() ?: 0L
            assertAll(
                { assertThat(positions).hasSize(threadCount) },
                { assertThat(positions.toSet()).hasSize(threadCount) },
                { assertThat(minPosition).isGreaterThanOrEqualTo(1L) },
                { assertThat(maxPosition).isLessThanOrEqualTo(threadCount.toLong()) },
            )
        }
    }
}
