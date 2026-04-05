package com.loopers.domain.queue

import com.loopers.infrastructure.scheduler.QueueScheduler
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class QueueConcurrencyIntegrationTest @Autowired constructor(
    private val queueRepository: QueueRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @MockBean
    private lateinit var queueScheduler: QueueScheduler

    companion object {
        private const val QUEUE_NAME = "concurrency-integration-test-queue"
        private const val BATCH_SIZE = 17L
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("처리량 초과 테스트")
    @Test
    fun `대기자가 배치 크기를 초과해도 한 번에 배치 크기만큼만 처리된다`() {
        // arrange: 30명 진입 (배치 크기 17 초과)
        repeat(30) { i ->
            queueRepository.enter(QUEUE_NAME, (i + 1).toLong(), System.currentTimeMillis().toDouble())
        }

        // act: 배치 크기(17)만큼 popMin — 스케줄러 1회의 핵심 동작
        val processed = queueRepository.popMin(QUEUE_NAME, BATCH_SIZE)

        // assert: 정확히 배치 크기만큼 처리됨, 나머지는 대기
        assertThat(processed).hasSize(BATCH_SIZE.toInt())
        assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(30L - BATCH_SIZE)

        // assert: score 순서로 정렬되어 있는지 확인
        val scores = processed.map { it.score }
        assertThat(scores).isSorted()
    }

    @DisplayName("동시 진입 테스트")
    @Test
    fun `여러 사용자가 동시에 대기열에 진입해도 모두 고유한 순번을 가진다`() {
        // arrange
        val threadCount = 50
        val startLatch = CountDownLatch(1) // 모든 스레드 시작을 동기화
        val completionLatch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            // act: 50개 스레드 동시 진입 (모두 준비 후 동시 시작)
            (1..threadCount).forEach { i ->
                executor.submit {
                    try {
                        // 모든 스레드가 준비될 때까지 대기
                        startLatch.await()

                        // 일부 스레드는 진입 전 지연 시뮬레이션 (느린 워커)
                        if (i % 10 == 0) {
                            Thread.sleep(5L)
                        }

                        queueRepository.enter(QUEUE_NAME, i.toLong(), System.currentTimeMillis().toDouble())
                    } finally {
                        completionLatch.countDown()
                    }
                }
            }

            // 모든 작업 제출 후 스레드들이 동시에 시작하도록 신호
            startLatch.countDown()

            // 모든 스레드가 완료될 때까지 대기
            val completed = completionLatch.await(10, TimeUnit.SECONDS)
            assertThat(completed)
                .withFailMessage("Threads did not complete within 10 seconds")
                .isTrue()

            // assert: 50명 모두 대기열에 존재 (중복 없음)
            assertThat(queueRepository.size(QUEUE_NAME)).isEqualTo(threadCount.toLong())

            // 각 userId의 순번이 고유한지 확인 (0 ~ 49)
            val ranks = (1..threadCount).map { i ->
                queueRepository.getRank(QUEUE_NAME, i.toLong())
            }
            assertThat(ranks).doesNotContainNull()
            assertThat(ranks.toSet()).hasSize(threadCount) // 모두 고유한 순번
        } finally {
            // cleanup: executor 종료 및 정상 종료 대기
            executor.shutdown()
            val terminated = executor.awaitTermination(5, TimeUnit.SECONDS)
            assertThat(terminated)
                .withFailMessage("Executor did not terminate within timeout")
                .isTrue()
        }
    }
}
