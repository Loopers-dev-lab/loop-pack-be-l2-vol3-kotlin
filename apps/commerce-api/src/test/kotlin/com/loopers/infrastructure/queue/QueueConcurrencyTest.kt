package com.loopers.infrastructure.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.waiting.model.EnterResult
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.interfaces.support.scheduler.OutboxRelayScheduler
import com.loopers.interfaces.support.scheduler.PaymentRecoveryScheduler
import com.loopers.interfaces.support.scheduler.QueueScheduler
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["spring.task.scheduling.enabled=false"],
)
class QueueConcurrencyTest @Autowired constructor(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @MockitoBean
    private lateinit var queueScheduler: QueueScheduler

    @MockitoBean
    private lateinit var paymentRecoveryScheduler: PaymentRecoveryScheduler

    @MockitoBean
    private lateinit var outboxRelayScheduler: OutboxRelayScheduler

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("100명 동시 진입 시 전원 성공하고 최종 순번이 중복 없이 보장된다")
    fun concurrentEnter_allSucceedAndFinalPositionsAreUnique() {
        // arrange
        val threadCount = 100
        val maxCapacity = 50_000
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val enteredUsers = java.util.concurrent.ConcurrentLinkedQueue<Long>()

        // act — 모든 스레드 준비 후 동시 시작
        (1L..threadCount.toLong()).forEach { userId ->
            executor.submit {
                try {
                    readyLatch.countDown()
                    startLatch.await()
                    val enterResult = waitingQueueRepository.enter(UserId(userId), maxCapacity)
                    if (enterResult is EnterResult.Entered) {
                        enteredUsers.add(userId)
                    }
                } finally {
                    doneLatch.countDown()
                }
            }
        }
        readyLatch.await()
        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        // assert — 전원 진입 성공
        assertThat(enteredUsers).hasSize(threadCount)
        assertThat(waitingQueueRepository.count()).isEqualTo(threadCount.toLong())

        // assert — 진입 완료 후 최종 순번 조회 시 중복 없음
        val finalPositions = (1L..threadCount.toLong()).map { userId ->
            waitingQueueRepository.findPosition(UserId(userId))
        }
        assertThat(finalPositions).doesNotContainNull()
        assertThat(finalPositions.toSet()).hasSize(threadCount)
    }
}
