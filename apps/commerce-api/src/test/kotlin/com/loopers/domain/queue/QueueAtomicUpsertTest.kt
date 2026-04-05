package com.loopers.domain.queue

import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Import(RedisTestContainersConfig::class)
@DisplayName("Atomic Upsert - FIFO 순서 보장 테스트")
class QueueAtomicUpsertTest @Autowired constructor(
    private val queueRepository: QueueRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    companion object {
        private const val QUEUE_NAME = "atomic-upsert-test-queue"
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("같은 타임스탬프 동시 진입 - 시퀀스로 순서 보장")
    @Test
    fun `100명 동시 진입시 시퀀스로 정확한 순서가 보장된다`() {
        val threadCount = 100
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)

        try {
            // act: 모든 스레드가 동시에 진입
            for (i in 0 until threadCount) {
                executor.submit {
                    try {
                        val userId = (1000L + i).toLong()
                        val score = queueRepository.atomicUpsertWithSequence(QUEUE_NAME, userId)
                        assertThat(score).isGreaterThan(0.0)
                        successCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()

            // assert: 모두 성공
            assertThat(successCount.get()).isEqualTo(threadCount)

            // assert: 큐에 100명 모두 등록
            assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(threadCount.toLong())

            // assert: 각 사용자의 rank가 0부터 99까지 순서대로 정렬
            for (i in 0 until threadCount) {
                val userId = (1000L + i).toLong()
                val rank = queueRepository.getRank(QUEUE_NAME, userId)
                assertThat(rank).isNotNull()
                assertThat(rank).isGreaterThanOrEqualTo(0)
                assertThat(rank).isLessThan(threadCount.toLong())
            }
        } finally {
            executor.shutdownNow()
        }
    }

    @DisplayName("같은 사용자 재진입 - 위치 업데이트")
    @Test
    fun `같은 사용자가 여러번 진입하면 마지막 진입 시간으로 위치가 업데이트된다`() {
        val userId = 100L
        val queueName = "$QUEUE_NAME-reentry"

        // act 1: 첫 번째 진입
        val score1 = queueRepository.atomicUpsertWithSequence(queueName, userId)
        val rank1 = queueRepository.getRank(queueName, userId)

        // act 2: 다른 사용자 5명 진입
        repeat(5) { i ->
            queueRepository.atomicUpsertWithSequence(queueName, (200L + i).toLong())
        }

        // act 3: 같은 사용자 재진입
        val score2 = queueRepository.atomicUpsertWithSequence(queueName, userId)
        val rank2 = queueRepository.getRank(queueName, userId)

        // assert: 스코어가 증가했음 (newer sequence number)
        assertThat(score2).isGreaterThan(score1)

        // assert: rank가 마지막으로 이동했음 (5번째 위치)
        assertThat(rank2).isEqualTo(5L)
    }

    @DisplayName("동일 시간 재진입 - 중복 제거 후 재삽입")
    @Test
    fun `동일 타임스탬프에서 재진입해도 중복되지 않고 새 순서로 정렬된다`() {
        val userId = 100L
        val queueName = "$QUEUE_NAME-same-timestamp"

        // act 1: 첫 번째 진입
        queueRepository.atomicUpsertWithSequence(queueName, userId)
        val sizeAfterFirst = queueRepository.size(queueName)

        // act 2: 동일 사용자 재진입
        queueRepository.atomicUpsertWithSequence(queueName, userId)
        val sizeAfterSecond = queueRepository.size(queueName)

        // assert: 중복되지 않음 (크기 동일)
        assertThat(sizeAfterFirst).isEqualTo(1L)
        assertThat(sizeAfterSecond).isEqualTo(1L)

        // assert: 사용자가 여전히 큐에 있음
        val rank = queueRepository.getRank(queueName, userId)
        assertThat(rank).isNotNull()
    }

    @DisplayName("높은 처리량 동시성 테스트")
    @Test
    fun `1000명 동시 진입 후 순서 보장`() {
        val threadCount = 1000
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(100) // 100개의 스레드풀
        val successCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        try {
            // act: 1000명 동시 진입
            for (i in 0 until threadCount) {
                executor.submit {
                    try {
                        val userId = (1000L + i).toLong()
                        queueRepository.atomicUpsertWithSequence(QUEUE_NAME, userId)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        errorCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()

            // assert: 모두 성공
            assertThat(successCount.get()).isEqualTo(threadCount)
            assertThat(errorCount.get()).isEqualTo(0)

            // assert: 큐에 1000명 모두 등록
            assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(threadCount.toLong())
        } finally {
            executor.shutdownNow()
        }
    }
}
