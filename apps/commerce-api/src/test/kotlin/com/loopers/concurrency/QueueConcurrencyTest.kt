package com.loopers.concurrency

import com.loopers.application.queue.EnterQueueUseCase
import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.application.queue.QueueStatus
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
@TestPropertySource(properties = ["queue.scheduler.interval-ms=999999"])
class QueueConcurrencyTest @Autowired constructor(
    private val enterQueueUseCase: EnterQueueUseCase,
    private val getQueuePositionUseCase: GetQueuePositionUseCase,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("1000명이 동시에 대기열에 진입하면 모두 고유한 순번을 받고 중복 없이 등록된다")
    @Test
    fun concurrentEnterShouldAssignUniquePositions() {
        val threadCount = 1000
        val actions = (1L..threadCount).map { userId ->
            { enterQueueUseCase.execute(userId) }
        }

        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }.map { it.getOrThrow() }
        val positions = successes.map { it.position }.toSet()

        assertAll(
            {
                assertThat(successes).`as`("1000명 모두 성공적으로 진입해야 한다")
                    .hasSize(threadCount.toInt())
            },
            {
                assertThat(positions).`as`("모든 순번이 고유해야 한다 (중복 없음)")
                    .hasSize(threadCount.toInt())
            },
        )
    }

    @DisplayName("동일 유저가 동시에 여러 번 진입해도 대기열에 1건만 등록된다")
    @Test
    fun duplicateUserConcurrentEnterShouldBeIdempotent() {
        val sameUserId = 1L
        val threadCount = 100
        val actions = (1..threadCount).map {
            { enterQueueUseCase.execute(sameUserId) }
        }

        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }.map { it.getOrThrow() }
        val positions = successes.map { it.position }.toSet()

        assertAll(
            {
                assertThat(successes).`as`("모든 요청이 성공해야 한다")
                    .hasSize(threadCount)
            },
            {
                assertThat(positions).`as`("동일 유저이므로 모두 같은 순번이어야 한다")
                    .hasSize(1)
            },
        )
    }

    @DisplayName("대기열 진입 후 순번 조회 시 WAITING 상태와 올바른 순번을 반환한다")
    @Test
    fun positionAfterEnterShouldReturnWaiting() {
        val threadCount = 100
        val actions = (1L..threadCount).map { userId ->
            { enterQueueUseCase.execute(userId) }
        }
        ConcurrencyTestHelper.executeConcurrently(actions)

        val positionResult = getQueuePositionUseCase.execute(50L)

        assertAll(
            { assertThat(positionResult.status).isEqualTo(QueueStatus.WAITING) },
            { assertThat(positionResult.position).isNotNull().isBetween(0L, threadCount.toLong() - 1) },
        )
    }
}
