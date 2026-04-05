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
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class QueueConcurrencyStressTest @Autowired constructor(
    private val queueRepository: QueueRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    companion object {
        private const val QUEUE_NAME = "stress-test-queue"
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("동시 진입 스트레스 테스트 - 반복 실행 안정성")
    @Test
    fun `100명 동시 진입을 5회 반복해도 모두 성공한다`() {
        repeat(5) { iteration ->
            // arrange: 매 반복마다 새로운 큐 이름 사용
            val iterationQueueName = "$QUEUE_NAME-$iteration"
            val threadCount = 100
            val latch = CountDownLatch(threadCount)
            val barrier = CyclicBarrier(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)

            try {
                // act: 100개 스레드 동시 진입 (모든 스레드가 준비 후 동시 시작)
                (1..threadCount).forEach { i ->
                    executor.submit {
                        try {
                            // 모든 스레드가 준비될 때까지 대기
                            barrier.await()

                            // 일부 스레드는 진입 전 지연 시뮬레이션 (느린 워커)
                            if (i % 10 == 0) {
                                Thread.sleep(10L)
                            }

                            queueRepository.enter(
                                iterationQueueName,
                                (iteration * threadCount + i).toLong(),
                                System.currentTimeMillis().toDouble(),
                            )
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                // assert: 모든 스레드가 완료되기를 기다림
                val completed = latch.await(30, TimeUnit.SECONDS)
                assertThat(completed)
                    .withFailMessage("Iteration $iteration: Threads did not complete within 30 seconds")
                    .isTrue()

                // assert: 모든 사용자가 진입했는지 확인
                val queueSize = queueRepository.size(iterationQueueName)
                assertThat(queueSize)
                    .withFailMessage("Iteration $iteration: Expected $threadCount users, got $queueSize")
                    .isEqualTo(threadCount.toLong())

                // assert: 모든 순번이 고유한지 확인
                val ranks = (1..threadCount).map { i ->
                    queueRepository.getRank(iterationQueueName, (iteration * threadCount + i).toLong())
                }
                assertThat(ranks).doesNotContainNull()
                assertThat(ranks.toSet())
                    .withFailMessage("Iteration $iteration: Duplicate ranks found")
                    .hasSize(threadCount)
            } finally {
                // cleanup: executor 종료 및 정상 종료 대기
                executor.shutdown()
                val terminated = executor.awaitTermination(10, TimeUnit.SECONDS)
                assertThat(terminated)
                    .withFailMessage("Iteration $iteration: Executor did not terminate within timeout")
                    .isTrue()

                redisCleanUp.truncateAll()
            }
        }
    }

    @DisplayName("동시 진입 - 높은 동시성")
    @Test
    fun `200명 동시 진입 시 모두 고유한 순번을 가진다`() {
        // arrange: 높은 동시성 테스트
        val threadCount = 200
        val latch = CountDownLatch(threadCount)
        val barrier = CyclicBarrier(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            // act: 모든 스레드가 준비 후 동시 시작
            (1..threadCount).forEach { i ->
                executor.submit {
                    try {
                        // 모든 스레드가 준비될 때까지 대기
                        barrier.await()

                        // 일부 스레드는 진입 전 지연 시뮬레이션 (느린 워커)
                        if (i % 20 == 0) {
                            Thread.sleep(15L)
                        }

                        queueRepository.enter(
                            QUEUE_NAME,
                            i.toLong(),
                            System.currentTimeMillis().toDouble(),
                        )
                    } finally {
                        latch.countDown()
                    }
                }
            }

            val completed = latch.await(30, TimeUnit.SECONDS)
            assertThat(completed)
                .withFailMessage("Threads did not complete within 30 seconds")
                .isTrue()

            // assert
            assertThat(queueRepository.size(QUEUE_NAME))
                .isEqualTo(threadCount.toLong())

            val ranks = (1..threadCount).map { i ->
                queueRepository.getRank(QUEUE_NAME, i.toLong())
            }
            assertThat(ranks).doesNotContainNull()
            assertThat(ranks.toSet()).hasSize(threadCount)
        } finally {
            // cleanup: executor 종료 및 정상 종료 대기
            executor.shutdown()
            val terminated = executor.awaitTermination(10, TimeUnit.SECONDS)
            assertThat(terminated)
                .withFailMessage("Executor did not terminate within timeout")
                .isTrue()
        }
    }

    @DisplayName("동시 진입 - 중복 진입 처리")
    @Test
    fun `같은 userId가 중복 진입 시 기존 진입이 갱신된다`() {
        // arrange: user 1이 가장 먼저 진입
        val baseScore = 1000.0
        queueRepository.enter(QUEUE_NAME, 1L, baseScore)
        val firstRank = queueRepository.getRank(QUEUE_NAME, 1L)
        assertThat(firstRank).isEqualTo(0L)

        // act: user 2, 3, 4가 더 늦게 진입
        queueRepository.enter(QUEUE_NAME, 2L, baseScore + 1)
        queueRepository.enter(QUEUE_NAME, 3L, baseScore + 2)
        queueRepository.enter(QUEUE_NAME, 4L, baseScore + 3)

        // user 1이 다시 진입하되, 가장 나중 사용자보다 늦게 진입
        queueRepository.enter(QUEUE_NAME, 1L, baseScore + 100)
        val secondRank = queueRepository.getRank(QUEUE_NAME, 1L)

        // assert: user 1의 순번이 뒤로 밀려야 함 (0 → 3으로 변경)
        assertThat(secondRank).isEqualTo(3L)
    }
}
