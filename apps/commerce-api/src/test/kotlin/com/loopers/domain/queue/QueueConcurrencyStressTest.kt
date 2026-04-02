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
            val executor = Executors.newFixedThreadPool(threadCount)

            // act: 100개 스레드 동시 진입
            (1..threadCount).forEach { i ->
                executor.submit {
                    try {
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
            executor.shutdown()

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

            // cleanup
            redisCleanUp.truncateAll()
        }
    }

    @DisplayName("동시 진입 - 높은 동시성")
    @Test
    fun `200명 동시 진입 시 모두 고유한 순번을 가진다`() {
        // arrange: 높은 동시성 테스트
        val threadCount = 200
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // act
        (1..threadCount).forEach { i ->
            executor.submit {
                try {
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
        assertThat(completed).isTrue()
        executor.shutdown()

        // assert
        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(threadCount.toLong())

        val ranks = (1..threadCount).map { i ->
            queueRepository.getRank(QUEUE_NAME, i.toLong())
        }
        assertThat(ranks).doesNotContainNull()
        assertThat(ranks.toSet()).hasSize(threadCount)
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
